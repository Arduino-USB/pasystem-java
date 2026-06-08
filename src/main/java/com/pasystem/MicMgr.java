package com.pasystem;

import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;

public class MicMgr {
    private final AudioFormat format;
    private final String target_mic;
    private TargetDataLine mic;
    private byte[] last_chunk;
    private volatile boolean running = true;
    private final Object lock = new Object();

    public MicMgr(String target_mic) {
        this.target_mic = target_mic;
        this.format = new AudioFormat(48000, 16, 1, true, false);
        this.last_chunk = new byte[4096];

        // Start the lifecycle management thread so it won't block the calling thread
        new Thread(this::manage_audio_lifecycle, "SoundMgr-Lifecycle").start();
    }

    /**
     * Scans the system and returns a list of names of all available 
     * audio capture devices (microphones) that support our format.
     */
    public static List<String> get_mics() {
        List<String> mic_names = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        
        AudioFormat check_format = new AudioFormat(48000, 16, 1, true, false);
        DataLine.Info line_info = new DataLine.Info(TargetDataLine.class, check_format);

        for (Mixer.Info mixer_info : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixer_info);
            if (mixer.isLineSupported(line_info)) {
                mic_names.add(mixer_info.getName());
            }
        }
        return mic_names;
    }

    /**
     * Forces the manager to drop the current connection (if any)
     * and restart the microphone search loop.
     */
    public void reconnect() {
        System.out.println("[SoundMgr] Manually triggering a reconnect...");
        cleanup_mic(); 
    }

    private void manage_audio_lifecycle() {
        while (running) {
            Mixer.Info selected_mixer_info = wait_for_mic();
            if (selected_mixer_info == null) continue; 

            try {
                Mixer mixer = AudioSystem.getMixer(selected_mixer_info);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                
                synchronized (lock) {
                    mic = (TargetDataLine) mixer.getLine(info);
                    mic.open(format);
                    mic.start();
                }
                System.out.println("[SoundMgr] Successfully connected to: " + selected_mixer_info.getName());

                stream_audio_data();

            } catch (LineUnavailableException e) {
                System.out.println("[SoundMgr] Line unavailable during setup. Retrying...");
                safe_sleep(2000);
            } finally {
                cleanup_mic();
            }
        }
    }

    private Mixer.Info wait_for_mic() {
        System.out.println("[SoundMgr] Waiting for mic: " + target_mic);
        while (running) {
            Mixer.Info info = find_mixer_by_name(target_mic);
            if (info != null) return info;
            
            safe_sleep(2000); 
        }
        return null;
    }

    private void stream_audio_data() {
        // Keep track of total silence time outside the loop
        long total_empty_time_ms = 0; 
        long max_timeout_ms = 2000; // 2 seconds

        while (running) {
            TargetDataLine current_mic;
            synchronized (lock) {
                current_mic = mic;
            }

            if (current_mic == null || !current_mic.isOpen()) {
                break;
            }

            int bytes_available = current_mic.available();

            // 1. Check if there isn't enough data available to read a full chunk
            if (bytes_available < last_chunk.length) {
                try {
                    Thread.sleep(10);
                    total_empty_time_ms += 10; // Track that we just waited 10ms
                } catch (InterruptedException e) {
                    break; 
                }

                // 2. Check if the cumulative waiting time has passed our 2-second limit
                if (total_empty_time_ms >= max_timeout_ms) {
                    System.out.println("[SoundMgr] Timeout: Microphone stopped responding.");
                    synchronized (lock) {
                        // Fill the array with zeros (silence)
                        java.util.Arrays.fill(last_chunk, (byte) 0);
                    }
                    break; // Break the while loop entirely
                }

                continue; // Go back to the top of the while loop to check bytes_available again
            }

            // 3. If we get past the 'if' block, it means data is available! 
            // Reset our timeout tracker back to 0 because the mic is alive.
            total_empty_time_ms = 0;

            // Since bytes_available >= last_chunk.length, this read() is guaranteed not to block
            int bytes_read = current_mic.read(last_chunk, 0, last_chunk.length);
            //System.out.println(bytes_read);
            
            if (bytes_read <= 0) {
                break;
            }
        }
    }
    private Mixer.Info find_mixer_by_name(String name) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        DataLine.Info line_info = new DataLine.Info(TargetDataLine.class, format);
        
        for (Mixer.Info mixer_info : mixers) {
            if (mixer_info.getName().toLowerCase().contains(name.toLowerCase())) {
                Mixer mixer = AudioSystem.getMixer(mixer_info);
                if (mixer.isLineSupported(line_info)) {
                    System.out.println("[SoundMgr] Found mic: " + mixer_info);
                    return mixer_info;
                }
            }
        }
        return null;
    }

    private void safe_sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.running = false;
        }
    }

    private void cleanup_mic() {
        synchronized (lock) {
            if (mic != null) {
                try {
                    mic.stop();
                    mic.close();
                } catch (Exception ignored) {}
                mic = null;
            }
        }
    }

    public synchronized byte[] pull() {
        return last_chunk;
    }

    public void close() {
        this.running = false;
        cleanup_mic();
    }
}