package com.pasystem.client;

import javax.sound.sampled.*;

import java.io.BufferedInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpeakerMgr {

    private static boolean instance_created = false;

    private static Mixer current_mixer;
    private static SourceDataLine speaker;

    private static final Object lock = new Object();

    private static volatile boolean running = false;

    private static Thread audio_thread;
    private static Thread reconnect_thread;

    /*
     * 100 packets is intentionally retained from the old implementation.
     *
     * At 960 bytes per packet this is only ~2 seconds of mono 48 kHz
     * 16-bit audio.
     */
    private static final LinkedBlockingQueue<byte[]> audio_queue =
        new LinkedBlockingQueue<>(100);

    private static final int CHUNK_SIZE = 960;

    private static final List<Clip> active_clips =
        new CopyOnWriteArrayList<>();


    // ============================================================
    // Constructor
    // ============================================================

    public SpeakerMgr() {

        synchronized (lock) {

            if (instance_created) {

                System.out.println(
                    "[SpeakerMgr] There can only be one given instance " +
                    "of SpeakerMgr, and then it is meant to be used " +
                    "as a static utility."
                );

                return;
            }

            instance_created = true;

            connect();
        }
    }


    // ============================================================
    // Get Available Speakers
    // ============================================================

	public static List<String> get_speakers() {
		List<String> speakerList = new ArrayList<>();
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		for (Mixer.Info info : mixers) {
		    try {
		        Mixer mixer = AudioSystem.getMixer(info);
		        // Speakers use source lines (output/playback lines)
		        Line.Info[] sourceLines = mixer.getSourceLineInfo();

		        if (sourceLines.length > 0) {
		            String name = info.getName();
		            String description = info.getDescription();
		            String combined = (name + " " + description).trim();

		            // Add the most descriptive representation available
		            if (!speakerList.contains(combined) && !speakerList.contains(name)) {
		                speakerList.add(combined);
		            }
		        }
		    } catch (Exception ignored) {
		    }
		}
		return speakerList;
	}


    // ============================================================
    // Find PulseAudio Mixer
    // ============================================================

    private static Mixer.Info find_pulse_mixer() {

        System.out.println(
            "[SpeakerMgr] Searching for PulseAudio mixer..."
        );

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {

            String name =
                info.getName().toLowerCase();

            String description =
                info.getDescription().toLowerCase();

            System.out.println(
                "[SpeakerMgr] Mixer: " +
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
    // Connect to PulseAudio
    // ============================================================

    private static void connect() {

        synchronized (lock) {

            /*
             * If something from a previous connection is still open,
             * close it before replacing it.
             */
            close_speaker_locked();

            try {

                Mixer.Info pulseInfo =
                    find_pulse_mixer();

                if (pulseInfo == null) {

                    System.out.println(
                        "[SpeakerMgr] PulseAudio MixerProvider " +
                        "was not found."
                    );

                    running = false;

                    return;
                }


                current_mixer =
                    AudioSystem.getMixer(pulseInfo);


                System.out.println(
                    "[SpeakerMgr] Using mixer: " +
                    pulseInfo.getName()
                );

                System.out.println(
                    "[SpeakerMgr] Description: " +
                    pulseInfo.getDescription()
                );


                /*
                 * Our network audio format.
                 *
                 * PulseAudio/PipeWire receives this stream.
                 *
                 * It is NOT opened against:
                 *
                 *     hw:Bar
                 *     plughw:Bar
                 *     ALSA directly
                 *
                 * The PulseAudio MixerProvider handles that.
                 */
                AudioFormat format =
                    new AudioFormat(
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
                        SourceDataLine.class,
                        format
                    );


                if (!current_mixer.isLineSupported(lineInfo)) {

                    System.out.println(
                        "[SpeakerMgr] PulseAudio mixer does not " +
                        "support SourceDataLine format: " +
                        format
                    );

                    current_mixer = null;
                    running = false;

                    return;
                }


                speaker =
                    (SourceDataLine)
                    current_mixer.getLine(lineInfo);


                /*
                 * Keep this buffer relatively small.
                 *
                 * 4096 bytes is about 42.7 ms at:
                 *
                 * 48000 Hz
                 * 16 bit
                 * mono
                 */
                speaker.open(format, 4096);

                speaker.start();


                audio_queue.clear();

                running = true;


                start_audio_thread();


                System.out.println(
                    "[SpeakerMgr] Successfully connected " +
                    "through PulseAudio/PipeWire"
                );

                System.out.println(
                    "[SpeakerMgr] Format: " +
                    format
                );
            }

            catch (Exception e) {

                System.out.println(
                    "[SpeakerMgr] Connection failed: " +
                    e.getMessage()
                );

                e.printStackTrace();

                close_speaker_locked();

                current_mixer = null;

                running = false;
            }
        }
    }


    // ============================================================
    // Start Audio Thread
    // ============================================================

    private static void start_audio_thread() {

        if (
            audio_thread != null &&
            audio_thread.isAlive()
        ) {
            return;
        }


        audio_thread =
            new Thread(
                SpeakerMgr::stream_audio_data,
                "SpeakerMgr-Audio"
            );


        audio_thread.setDaemon(true);

        audio_thread.start();
    }


    // ============================================================
    // Push Audio
    // ============================================================

    public static void push(byte[] chunk) {

        if (!instance_created || !running) {
            return;
        }

        if (chunk == null || chunk.length == 0) {
            return;
        }


        /*
         * Clone because the network layer may reuse its buffer.
         */
        byte[] copy =
            chunk.clone();


        /*
         * Keep the real-time behavior from the old implementation:
         *
         * If the queue is full, throw away the oldest packet.
         *
         * This prevents latency from continually growing.
         */
        if (!audio_queue.offer(copy)) {

            audio_queue.poll();

            audio_queue.offer(copy);
        }
    }


    // ============================================================
    // Audio Thread
    // ============================================================

    private static void stream_audio_data() {

        System.out.println(
            "[SpeakerMgr] Audio thread started"
        );


        while (running) {

            SourceDataLine current_speaker;

            synchronized (lock) {
                current_speaker = speaker;
            }


            if (
                current_speaker == null ||
                !current_speaker.isOpen()
            ) {

                return;
            }


            try {

                /*
                 * IMPORTANT:
                 *
                 * Do NOT use:
                 *
                 *     speaker.available()
                 *
                 *     Thread.sleep(10)
                 *
                 *     timeout after 2 seconds
                 *
                 * PulseAudio's SourceDataLine is a streaming
                 * interface. Just give it audio.
                 *
                 * take() blocks until a network packet arrives.
                 */
                byte[] chunk =
                    audio_queue.take();


                /*
                 * Write directly to the PulseAudio stream.
                 *
                 * write() may block until the PulseAudio backend
                 * accepts the data, which is exactly what we want.
                 */
                current_speaker.write(
                    chunk,
                    0,
                    chunk.length
                );
            }

            catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                return;
            }

            catch (Exception e) {

                System.out.println(
                    "[SpeakerMgr] Audio stream error: " +
                    e.getMessage()
                );

                handle_disconnect();

                return;
            }
        }
    }


    // ============================================================
    // Disconnect
    // ============================================================

    private static void handle_disconnect() {

        synchronized (lock) {

            if (!running) {
                return;
            }

            System.out.println(
                "[SpeakerMgr] Audio connection lost"
            );


            running = false;


            /*
             * Wake up the audio thread if it is blocked in take().
             */
            if (audio_thread != null) {
                audio_thread.interrupt();
                audio_thread = null;
            }


            for (Clip clip : active_clips) {

                try {
                    clip.stop();
                    clip.close();
                }

                catch (Exception ignored) {
                }
            }


            active_clips.clear();


            close_speaker_locked();

            current_mixer = null;
        }


        start_reconnect_thread();
    }


    // ============================================================
    // Close Speaker
    // ============================================================

    private static void close_speaker_locked() {

        if (speaker == null) {
            return;
        }


        try {
            speaker.stop();
        }

        catch (Exception ignored) {
        }


        try {
            speaker.close();
        }

        catch (Exception ignored) {
        }


        speaker = null;
    }


    // ============================================================
    // Reconnect
    // ============================================================

    private static void start_reconnect_thread() {

        synchronized (lock) {

            if (
                reconnect_thread != null &&
                reconnect_thread.isAlive()
            ) {
                return;
            }


            reconnect_thread =
                new Thread(
                    () -> {

                        System.out.println(
                            "[SpeakerMgr] Attempting to reconnect..."
                        );


                        while (!running) {

                            try {

                                Thread.sleep(2000);

                                connect();

                            }

                            catch (InterruptedException e) {

                                Thread.currentThread().interrupt();

                                return;
                            }
                        }

                    },
                    "SpeakerMgr-Reconnect"
                );


            reconnect_thread.setDaemon(true);

            reconnect_thread.start();
        }
    }


    // ============================================================
    // Play WAV
    // ============================================================

    public static Clip play_file(InputStream input) {

        synchronized (lock) {

            if (
                current_mixer == null ||
                speaker == null ||
                !speaker.isOpen()
            ) {

                System.out.println(
                    "[SpeakerMgr] Speaker isn't up, " +
                    "can't play alert"
                );

                return null;
            }


            try {

                BufferedInputStream buffered =
                    new BufferedInputStream(input);


                AudioInputStream stream =
                    AudioSystem.getAudioInputStream(buffered);


                System.out.println(
                    "[SpeakerMgr] WAV format: " +
                    stream.getFormat()
                );

                System.out.println(
                    "[SpeakerMgr] WAV frames: " +
                    stream.getFrameLength()
                );

                System.out.println(
                    "[SpeakerMgr] Mixer: " +
                    current_mixer.getMixerInfo().getName()
                );


                /*
                 * VERY IMPORTANT:
                 *
                 * Get the Clip from the PulseAudio mixer,
                 * not AudioSystem.getClip().
                 *
                 * Therefore:
                 *
                 * Java
                 *   ↓
                 * PulseAudio MixerProvider
                 *   ↓
                 * PulseAudio
                 *   ↓
                 * PipeWire
                 *   ↓
                 * CA-2890PRO
                 */
                DataLine.Info clipInfo =
                    new DataLine.Info(
                        Clip.class,
                        stream.getFormat()
                    );


                if (!current_mixer.isLineSupported(clipInfo)) {

                    System.out.println(
                        "[SpeakerMgr] PulseAudio mixer does not " +
                        "support Clip format: " +
                        stream.getFormat()
                    );

                    stream.close();

                    return null;
                }


                Clip clip =
                    (Clip)
                    current_mixer.getLine(clipInfo);


                clip.open(stream);


                stream.close();


                clip.setFramePosition(0);


                active_clips.add(clip);


                clip.addLineListener(
                    event -> {

                        if (
                            event.getType() ==
                            LineEvent.Type.CLOSE
                        ) {

                            active_clips.remove(clip);
                        }
                    }
                );
				
				clip.start();

				clip.loop(Clip.LOOP_CONTINUOUSLY);
                return clip;
            }

            catch (Exception e) {

                System.out.println(
                    "[SpeakerMgr] Failed to play sound stream: " +
                    e.getMessage()
                );

                e.printStackTrace();

                return null;
            }
        }
    }


    // ============================================================
    // Status
    // ============================================================

    public static boolean is_up() {

        synchronized (lock) {

            return
                speaker != null &&
                speaker.isOpen() &&
                running;
        }
    }
}
