package com.pasystem;

import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;

public class MicMgr {
    private static boolean instance_created = false;

    private static AudioFormat format;
    private static String target_mic;
    private static TargetDataLine mic;
    private static byte[] last_chunk;
    private static volatile boolean running = true;
    private static final Object lock = new Object();

    // Constructor acts as the one-time static initializer
    public MicMgr(String target_mic) {
        synchronized (lock) {
            if (instance_created) {
                System.out.println("[MicMgr] There can only be one given instance of MicMgr, and then it is meant to be used as a static utility.");
                return;
            }
            MicMgr.target_mic = target_mic;
            format = new AudioFormat(48000, 16, 1, true, false);
            last_chunk = new byte[960];
            instance_created = true;

            // Start the lifecycle management thread so it won't block the calling thread
            new Thread(MicMgr::manage_audio_lifecycle, "SoundMgr-Lifecycle").start();
        }
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
            try {
                Mixer mixer = AudioSystem.getMixer(mixer_info);
                if (mixer.isLineSupported(line_info)) {
                    mic_names.add(mixer_info.getName());
                }
            } catch (Exception ignored) {}
        }
        return mic_names;
    }

    /**
     * Forces the manager to drop the current connection (if any)
     * and restart the microphone search loop.
     */
    public static void reconnect() {
        if (!instance_created) return;
        System.out.println("[SoundMgr] Manually triggering a reconnect...");
        cleanup_mic(); 
    }

    private static void manage_audio_lifecycle() {
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

    private static Mixer.Info wait_for_mic() {
        System.out.println("[SoundMgr] Waiting for mic: " + target_mic);
        while (running) {
            Mixer.Info info = find_mixer_by_name(target_mic);
            if (info != null) return info;
            
            safe_sleep(2000); 
        }
        return null;
    }

    private static void stream_audio_data() {
        long total_empty_time_ms = 0; 
        long max_timeout_ms = 2000; 

        while (running) {
            TargetDataLine current_mic;
            synchronized (lock) {
                current_mic = mic;
            }

            if (current_mic == null || !current_mic.isOpen()) {
                break;
            }

            int bytes_available = current_mic.available();

            if (bytes_available < last_chunk.length) {
                try {
                    Thread.sleep(10);
                    total_empty_time_ms += 10;
                } catch (InterruptedException e) {
                    break; 
                }

                if (total_empty_time_ms >= max_timeout_ms) {
                    System.out.println("[SoundMgr] Timeout: Microphone stopped responding.");
                    synchronized (lock) {
                        java.util.Arrays.fill(last_chunk, (byte) 0);
                    }
                    break;
                }
                continue;
            }

            total_empty_time_ms = 0;

            int bytes_read = current_mic.read(last_chunk, 0, last_chunk.length);
            
            if (bytes_read <= 0) {
                break;
            }
        }
    }

    private static Mixer.Info find_mixer_by_name(String name) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        DataLine.Info line_info = new DataLine.Info(TargetDataLine.class, format);
        
        for (Mixer.Info mixer_info : mixers) {
            if (mixer_info.getName().toLowerCase().contains(name.toLowerCase())) {
                try {
                    Mixer mixer = AudioSystem.getMixer(mixer_info);
                    if (mixer.isLineSupported(line_info)) {
                        System.out.println("[SoundMgr] Found mic: " + mixer_info);
                        return mixer_info;
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static void safe_sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private static void cleanup_mic() {
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

    public static byte[] pull() {
        synchronized (lock) {
            return last_chunk;
        }
    }

    public static void close() {
        running = false;
        cleanup_mic();
    }
}
