package com.pasystem.client;

import javax.sound.sampled.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MicMgr {

	private static boolean instance_created = false;

	private static AudioFormat format;
	private static String target_mic;

	private static Mixer current_mixer;
	private static TargetDataLine mic;

	private static volatile boolean running = false;

	private static final Object lock = new Object();

	private static Thread lifecycle_thread;

	/*
	 * 960 bytes =
	 *
	 * 48000 Hz
	 * 16-bit
	 * mono
	 *
	 * 480 samples × 2 bytes = 960 bytes
	 *
	 * Therefore each packet represents exactly 10 ms
	 * of audio.
	 */
	private static final int CHUNK_SIZE = 960;


	/*
	 * Keep a small amount of buffering.
	 *
	 * 100 packets × 10 ms = approximately 1 second
	 * of audio.
	 *
	 * If the consumer falls behind, we drop the oldest
	 * packet rather than allowing latency to grow forever.
	 */
	private static final BlockingQueue<byte[]> audio_queue =
		new LinkedBlockingQueue<>(100);


	/*
	 * Used to wake pull() when MicMgr is closed.
	 *
	 * A null value is never placed into the BlockingQueue,
	 * so pull() needs another way to know that shutdown
	 * occurred.
	 */
	private static volatile boolean closed = false;


	// ============================================================
	// Constructor
	// ============================================================

	public MicMgr(String target_mic) {

		synchronized (lock) {

			if (instance_created) {

				System.out.println(
					"[MicMgr] There can only be one given instance " +
					"of MicMgr, and then it is meant to be used " +
					"as a static utility."
				);

				return;
			}


			MicMgr.target_mic = target_mic;


			/*
			 * Network/capture format.
			 *
			 * PCM_SIGNED
			 * 48000 Hz
			 * 16-bit
			 * mono
			 * 2 bytes/frame
			 * little-endian
			 */
			format =
				new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					48000,
					16,
					1,
					2,
					48000,
					false
				);


			instance_created = true;

			running = true;

			closed = false;


			lifecycle_thread =
				new Thread(
					MicMgr::manage_audio_lifecycle,
					"MicMgr-Lifecycle"
				);

			lifecycle_thread.setDaemon(true);

			lifecycle_thread.start();
		}
	}


	// ============================================================
	// Get Available Microphones
	// ============================================================

	public static List<String> get_mics() {

		List<String> mic_names =
			new ArrayList<>();


		Mixer.Info pulseInfo =
			find_pulse_mixer();


		if (pulseInfo == null) {

			System.out.println(
				"[MicMgr] PulseAudio MixerProvider was not found."
			);

			return mic_names;
		}


		try {

			Mixer mixer =
				AudioSystem.getMixer(pulseInfo);


			AudioFormat testFormat =
				format != null
					? format
					: new AudioFormat(
						AudioFormat.Encoding.PCM_SIGNED,
						48000,
						16,
						1,
						2,
						48000,
						false
					);


			DataLine.Info lineInfo =
				new DataLine.Info(
					TargetDataLine.class,
					testFormat
				);


			if (mixer.isLineSupported(lineInfo)) {

				/*
				 * PulseAudio provides one logical capture mixer.
				 *
				 * The actual PipeWire/PulseAudio source is selected
				 * by the PulseAudio environment rather than by opening
				 * an ALSA hw device directly.
				 */
				mic_names.add(pulseInfo.getName());
			}

		}

		catch (Exception e) {

			System.out.println(
				"[MicMgr] Failed to inspect microphones: " +
				e.getMessage()
			);
		}


		return mic_names;
	}


	// ============================================================
	// Find PulseAudio Mixer
	// ============================================================

	private static Mixer.Info find_pulse_mixer() {

		System.out.println(
			"[MicMgr] Searching for PulseAudio mixer..."
		);


		for (Mixer.Info info : AudioSystem.getMixerInfo()) {

			String name =
				info.getName().toLowerCase();

			String description =
				info.getDescription().toLowerCase();


			System.out.println(
				"[MicMgr] Mixer: " +
				info.getName() +
				" | " +
				info.getDescription()
			);


			if (
				name.contains("pulseaudio") ||
				name.contains("pulse audio") ||
				description.contains("pulseaudio") ||
				description.contains("pulse audio")
			) {

				return info;
			}
		}


		return null;
	}


	// ============================================================
	// Reconnect
	// ============================================================

	public static void reconnect() {

		if (!instance_created) {
			return;
		}


		System.out.println(
			"[MicMgr] Manually triggering a reconnect..."
		);


		cleanup_mic();
	}


	// ============================================================
	// Audio Lifecycle
	// ============================================================

	private static void manage_audio_lifecycle() {

		while (running) {

			try {

				Mixer.Info pulseInfo =
					wait_for_pulse_mixer();


				if (pulseInfo == null) {
					continue;
				}


				Mixer mixer =
					AudioSystem.getMixer(pulseInfo);


				DataLine.Info lineInfo =
					new DataLine.Info(
						TargetDataLine.class,
						format
					);


				if (!mixer.isLineSupported(lineInfo)) {

					System.out.println(
						"[MicMgr] PulseAudio mixer does not " +
						"support TargetDataLine format: " +
						format
					);

					safe_sleep(2000);

					continue;
				}


				TargetDataLine newMic;


				synchronized (lock) {

					current_mixer = mixer;


					newMic =
						(TargetDataLine)
						mixer.getLine(lineInfo);


					/*
					 * Open the capture stream through:
					 *
					 * Java
					 *   ↓
					 * PulseAudio MixerProvider
					 *   ↓
					 * PulseAudio
					 *   ↓
					 * PipeWire
					 *   ↓
					 * Microphone
					 */
					newMic.open(
						format,
						4096
					);


					newMic.start();


					mic = newMic;
				}


				System.out.println(
					"[MicMgr] Successfully connected through " +
					"PulseAudio/PipeWire"
				);


				System.out.println(
					"[MicMgr] Format: " +
					format
				);


				System.out.println(
					"[MicMgr] Input device: " +
					target_mic
				);


				/*
				 * Capture until the line fails or MicMgr is closed.
				 */
				stream_audio_data(newMic);

			}

			catch (LineUnavailableException e) {

				System.out.println(
					"[MicMgr] Microphone line unavailable. " +
					"Retrying..."
				);

			}

			catch (Exception e) {

				if (running) {

					System.out.println(
						"[MicMgr] Microphone error: " +
						e.getMessage()
					);
				}
			}

			finally {

				cleanup_mic();
			}


			if (running) {
				safe_sleep(2000);
			}
		}
	}


	// ============================================================
	// Wait for PulseAudio
	// ============================================================

	private static Mixer.Info wait_for_pulse_mixer() {

		System.out.println(
			"[MicMgr] Waiting for PulseAudio microphone: " +
			target_mic
		);


		while (running) {

			Mixer.Info info =
				find_pulse_mixer();


			if (info != null) {

				System.out.println(
					"[MicMgr] Found PulseAudio microphone mixer: " +
					info.getName()
				);

				return info;
			}


			safe_sleep(2000);
		}


		return null;
	}


	// ============================================================
	// Capture Audio
	// ============================================================

	private static void stream_audio_data(
		TargetDataLine current_mic
	) {

		System.out.println(
			"[MicMgr] Audio capture thread started"
		);


		while (
			running &&
			current_mic != null &&
			current_mic.isOpen()
		) {

			try {

				/*
				 * IMPORTANT:
				 *
				 * Create a NEW buffer for every audio packet.
				 *
				 * This completely eliminates the old race where
				 * pull() could return the same array that read()
				 * was simultaneously modifying.
				 */
				byte[] chunk =
					new byte[CHUNK_SIZE];


				/*
				 * TargetDataLine.read() may theoretically return
				 * fewer bytes than requested.
				 *
				 * Keep reading until the entire 960-byte packet
				 * has been filled.
				 */
				int totalRead = 0;


				while (
					totalRead < CHUNK_SIZE &&
					running &&
					current_mic.isOpen()
				) {

					int bytesRead =
						current_mic.read(
							chunk,
							totalRead,
							CHUNK_SIZE - totalRead
						);


					if (bytesRead <= 0) {

						System.out.println(
							"[MicMgr] Microphone read returned " +
							bytesRead
						);

						break;
					}


					totalRead += bytesRead;
				}


				if (!running) {
					break;
				}


				/*
				 * Never expose a partial packet.
				 */
				if (totalRead != CHUNK_SIZE) {

					System.out.println(
						"[MicMgr] Incomplete audio packet: " +
						totalRead +
						"/" +
						CHUNK_SIZE
					);

					continue;
				}


				/*
				 * At this point the buffer is COMPLETE.
				 *
				 * Nobody will ever modify it again.
				 *
				 * Put the complete packet into the queue.
				 */
				if (!audio_queue.offer(chunk)) {

					/*
					 * Queue full.
					 *
					 * Drop oldest packet to prevent latency buildup.
					 */
					audio_queue.poll();

					audio_queue.offer(chunk);
				}
			}

			catch (Exception e) {

				if (running) {

					System.out.println(
						"[MicMgr] Microphone stream error: " +
						e.getMessage()
					);
				}

				return;
			}
		}
	}


	// ============================================================
	// Get Audio Chunk
	// ============================================================

	public static byte[] pull() {

		/*
		 * Wait until a COMPLETE audio packet exists.
		 *
		 * Unlike the old implementation, this does NOT return
		 * a buffer currently being modified by the capture thread.
		 */
		while (running && !closed) {

			try {

				byte[] chunk =
					audio_queue.take();


				/*
				 * This is a complete, independently-owned
				 * 960-byte audio packet.
				 */
				return chunk;
			}

			catch (InterruptedException e) {

				Thread.currentThread().interrupt();

				return null;
			}
		}


		return null;
	}


	// ============================================================
	// Cleanup
	// ============================================================

	private static void cleanup_mic() {

		synchronized (lock) {

			if (mic != null) {

				try {
					mic.stop();
				}

				catch (Exception ignored) {
				}


				try {
					mic.close();
				}

				catch (Exception ignored) {
				}


				mic = null;
			}


			current_mixer = null;
		}
	}


	// ============================================================
	// Sleep
	// ============================================================

	private static void safe_sleep(int millis) {

		try {

			Thread.sleep(millis);

		}

		catch (InterruptedException e) {

			Thread.currentThread().interrupt();
		}
	}


	// ============================================================
	// Status
	// ============================================================

	public static boolean is_up() {

		synchronized (lock) {

			return
				mic != null &&
				mic.isOpen() &&
				running;
		}
	}


	// ============================================================
	// Close
	// ============================================================

	public static void close() {

		running = false;

		closed = true;


		if (lifecycle_thread != null) {

			lifecycle_thread.interrupt();
		}


		cleanup_mic();


		/*
		 * Clear buffered audio so old audio isn't played if
		 * the manager is somehow reused.
		 */
		audio_queue.clear();
	}
}
