package com.pasystem;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpeakerMgr {
    private SourceDataLine speaker;
    private final Object lock = new Object();
    private boolean running = false;
    private final String deviceName;
    
    private final byte[] write_chunk = new byte[4096];
    private boolean has_new_data = false;

    public SpeakerMgr(String deviceName) {
        this.deviceName = deviceName;
        connect();
    }

    // --- Get Available Speakers ---
    public List<String> get_speakers() {
        List<String> speakerList = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        
        for (Mixer.Info info : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                // Check if this mixer supports output lines (speakers)
                Line.Info[] sourceLineInfo = mixer.getSourceLineInfo();
                if (sourceLineInfo.length > 0) {
                    speakerList.add(info.getName());
                }
            } catch (Exception ignored) {
                // Skip any mixers that fail to open/query
            }
        }
        return speakerList;
    }

    // --- Core Connection Logic ---
    private void connect() {
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
                
                new Thread(this::stream_audio_data).start();
                System.out.println("[SpeakerMgr] Successfully connected to: " + deviceName);
                
            } catch (Exception e) {
                System.out.println("[SpeakerMgr] Connection failed: " + e.getMessage());
                speaker = null;
                running = false;
            }
        }
    }

    // --- The Push Function ---
    public void push(byte[] chunk) {
        synchronized (lock) {
            int length = Math.min(chunk.length, write_chunk.length);
            System.arraycopy(chunk, 0, write_chunk, 0, length);
            has_new_data = true;
            lock.notifyAll(); 
        }
    }

    // --- Background Playback Loop ---
    private void stream_audio_data() {
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
    private void handle_disconnect() {
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