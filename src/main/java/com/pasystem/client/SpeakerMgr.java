package com.pasystem.client;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpeakerMgr {
    private static boolean instance_created = false;
    private static Mixer current_mixer;
    private static SourceDataLine speaker;
    private static final Object lock = new Object();
    private static boolean running = false;
    private static String deviceName;
    
    // Smooth FIFO shock absorber to prevent back-to-back packets from overwriting each other
    private static final LinkedBlockingQueue<byte[]> audio_queue = new LinkedBlockingQueue<>(100);
    private static final int CHUNK_SIZE = 960;
	
	private static final List<Clip> active_clips = new CopyOnWriteArrayList<>();

    // Constructor acts as the one-time static initializer
    public SpeakerMgr(String deviceName) {
        synchronized (lock) {
            if (instance_created) {
                System.out.println("[SpeakerMgr] There can only be one given instance of SpeakerMgr, and then it is meant to be used as a static utility.");
                return;
            }    
            SpeakerMgr.deviceName = deviceName;
            instance_created = true;
            connect();
        }
    }

    // --- Get Available Speakers ---
    public static List<String> get_speakers() {
        List<String> speakerList = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        
        for (Mixer.Info info : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info[] sourceLineInfo = mixer.getSourceLineInfo();
                if (sourceLineInfo.length > 0) {
                    speakerList.add(info.getName());
                }
            } catch (Exception ignored) {}
        }
        return speakerList;
    }

    // --- Core Connection Logic ---
    private static void connect() {
        synchronized (lock) {
            try {
                Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                Mixer.Info targetMixer = null;
                
                for (Mixer.Info info : mixers) {
                    if (info.getName().contains(deviceName)) {
                        targetMixer = info;
                        break;
                    }
                }

                if (targetMixer == null) {
                    System.out.println("[SpeakerMgr] Waiting for speaker: " + deviceName);
                    return;
                }

                AudioFormat format = new AudioFormat(48000, 16, 1, true, false);
                Mixer mixer = AudioSystem.getMixer(targetMixer);
                
                DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
                speaker = (SourceDataLine) mixer.getLine(lineInfo);
                speaker.open(format, 4096); // internal line hardware buffer sizing
                speaker.start();
                
                running = true;
                audio_queue.clear();
                
                new Thread(SpeakerMgr::stream_audio_data).start();
                System.out.println("[SpeakerMgr] Successfully connected to: " + deviceName);
                
            } catch (Exception e) {
                System.out.println("[SpeakerMgr] Connection failed: " + e.getMessage());
                speaker = null;
                running = false;
            }
        }
    }

    // --- The Push Function ---
    public static void push(byte[] chunk) {
        if (!instance_created) return; 
        
        // Offer to the queue. If it's completely full, drop the oldest so playback stays in real-time
        if (!audio_queue.offer(chunk.clone())) {
            audio_queue.poll();
            audio_queue.offer(chunk.clone());
        }
    }

    // --- Background Playback Loop ---
    private static void stream_audio_data() {
        long total_empty_time_ms = 0;
        long max_timeout_ms = 2000; 
        byte[] silence = new byte[CHUNK_SIZE];

        while (running) {
            SourceDataLine current_speaker;
            synchronized (lock) {
                current_speaker = speaker;
            }

            if (current_speaker == null || !current_speaker.isOpen()) {
                break;
            }

            int spaces_available = current_speaker.available();

            if (spaces_available < CHUNK_SIZE) {
                try {
                    Thread.sleep(10);
                    total_empty_time_ms += 10;
                } catch (InterruptedException e) {
                    break;
                }

                if (total_empty_time_ms >= max_timeout_ms) {
                    System.out.println("[SpeakerMgr] Timeout: Speaker stopped responding.");
                    handle_disconnect();
                    break;
                }
                continue;
            }

            total_empty_time_ms = 0;

            // Poll data cleanly out of our concurrent queue buffer
            byte[] chunk = audio_queue.poll();
            
            if (chunk != null) {
                current_speaker.write(chunk, 0, chunk.length);
            } else {
                // Buffer is starving for packets, write a clean slice of silence
                current_speaker.write(silence, 0, silence.length);
                try {
                    Thread.sleep(5); // brief sleep to let incoming network fragments populate the queue
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    // --- Reconnection Logic ---
	private static void handle_disconnect() {
		synchronized (lock) {
			running = false;

			for (Clip clip : active_clips) {
				try {
					clip.stop();
					clip.close();
				} catch (Exception ignored) {}
			}

			active_clips.clear();

			if (speaker != null) {
				try {
					speaker.stop();
					speaker.close();
				} catch (Exception ignored) {}
				speaker = null;
			}
		}

		new Thread(() -> {
			System.out.println("[SpeakerMgr] Attempting to reconnect...");
			while (!running) {
				try {
					Thread.sleep(2000);
					connect();
				} catch (InterruptedException e) {
					break;
				}
			}
		}).start();
	}
	
	public static Clip play_file(InputStream input) {
		synchronized (lock) {
			if (speaker == null || !speaker.isOpen()) {
				System.out.println("[SpeakerMgr] Speaker isnt up, can't play alert");
				return null;
			}

			try {
				BufferedInputStream buffered =
						new BufferedInputStream(input);

				AudioInputStream stream =
						AudioSystem.getAudioInputStream(buffered);

				Clip clip = AudioSystem.getClip();

				clip.open(stream);

				stream.close(); // IMPORTANT FIX

				clip.setFramePosition(0); // ensure restart-safe
				clip.loop(Clip.LOOP_CONTINUOUSLY);

				active_clips.add(clip);

				clip.addLineListener(event -> {
					if (event.getType() == LineEvent.Type.CLOSE) {
						active_clips.remove(clip);
					}
				});

				return clip;

			} catch (Exception e) {
				System.out.println("[SpeakerMgr] Failed to play alert: " + e.getMessage());
				return null;
			}
		}
	}

		
	public static boolean is_up() {
		synchronized (lock) {
			return speaker != null
					&& speaker.isOpen()
					&& running;
		}
	}
}
