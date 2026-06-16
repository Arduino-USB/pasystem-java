package com.pasystem.client;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class SpeakerMgr {
    private static boolean instance_created = false;
    
    private static SourceDataLine speaker;
    private static final Object lock = new Object();
    private static boolean running = false;
    private static String deviceName;
    
    private static final byte[] write_chunk = new byte[960];
    private static boolean has_new_data = false;

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
                speaker.open(format);
                speaker.start();
                
                running = true;
                has_new_data = false;
                
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
        synchronized (lock) {
            if (!instance_created) return; 
            
            int length = Math.min(chunk.length, write_chunk.length);
            System.arraycopy(chunk, 0, write_chunk, 0, length);
            has_new_data = true;
            lock.notifyAll(); 
        }
    }

    // --- Background Playback Loop ---
    private static void stream_audio_data() {
        long total_empty_time_ms = 0;
        long max_timeout_ms = 2000; 

        while (running) {
            SourceDataLine current_speaker;
            synchronized (lock) {
                current_speaker = speaker;
                
                if (!has_new_data && running) {
                    try {
                        lock.wait(10); 
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            if (current_speaker == null || !current_speaker.isOpen()) {
                break;
            }

            int spaces_available = current_speaker.available();

            if (spaces_available < write_chunk.length) {
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

            synchronized (lock) {
                if (has_new_data) {
                    current_speaker.write(write_chunk, 0, write_chunk.length);
                    has_new_data = false; 
                } else {
                    byte[] silence = new byte[write_chunk.length];
                    current_speaker.write(silence, 0, silence.length);
                }
            }
        }
    }

    // --- Reconnection Logic ---
    private static void handle_disconnect() {
        synchronized (lock) {
            running = false;
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
}
