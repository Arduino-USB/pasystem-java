package com.pasystem.client;

import javax.sound.sampled.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MicMgr {

    private static boolean instance_created = false;

    private static AudioFormat format;

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
    // Initialization (Static Start)
    // ============================================================

    static {
        // Network/capture format.
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
    }

    public MicMgr() {
        synchronized (lock) {

            if (instance_created) {

                System.out.println(
                    "[MicMgr] There can only be one given instance " +
                    "of MicMgr, and then it is meant to be used " +
                    "as a static utility."
                );

                return;
            }

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
        List<String> micList = new ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info info : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info[] targetLines = mixer.getTargetLineInfo();

                if (targetLines.length > 0) {
                    String name = info.getName();
                    String description = info.getDescription();
                    String combined = (name + " " + description).trim();

                    if (!micList.contains(combined) && !micList.contains(name)) {
                        micList.add(combined);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return micList;
    }


    // ============================================================
    // Find PulseAudio Mixer
    // ============================================================

    private static Mixer.Info find_mic_mixer() {

        System.out.println("[MicMgr] Searching for PulseAudio mixer...");

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {

            String name = info.getName().toLowerCase();
            String description = info.getDescription().toLowerCase();

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
                try {
                    Mixer mixer = AudioSystem.getMixer(info);
                    if (mixer.getTargetLineInfo().length > 0) {
                        return info;
                    }
                } catch (Exception ignored) {
                }
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
                    wait_for_mic_mixer();


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
                     * Open the capture stream through the selected mixer provider.
                     */
                    newMic.open(
                        format,
                        4096
                    );


                    newMic.start();


                    mic = newMic;
                }


                System.out.println(
                    "[MicMgr] Successfully connected to PulseAudio mixer: " +
                    pulseInfo.getName()
                );


                System.out.println(
                    "[MicMgr] Format: " +
                    format
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
    // Wait for Microphone Mixer
    // ============================================================

    private static Mixer.Info wait_for_mic_mixer() {

        System.out.println("[MicMgr] Waiting for PulseAudio microphone...");


        while (running) {

            Mixer.Info info =
                find_mic_mixer();


            if (info != null) {

                System.out.println(
                    "[MicMgr] Found PulseAudio mixer: " +
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

                byte[] chunk =
                    new byte[CHUNK_SIZE];


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


                if (totalRead != CHUNK_SIZE) {

                    System.out.println(
                        "[MicMgr] Incomplete audio packet: " +
                        totalRead +
                        "/" +
                        CHUNK_SIZE
                    );

                    continue;
                }


                if (!audio_queue.offer(chunk)) {

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

        while (running && !closed) {

            try {

                byte[] chunk =
                    audio_queue.take();

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
    // Cleanup Mic
    // ============================================================

    private static void cleanup_mic() {

        synchronized (lock) {

            if (mic == null) {
                return;
            }


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
            current_mixer = null;
        }
    }


    // ============================================================
    // Close / Shutdown
    // ============================================================

    public static void close() {

        synchronized (lock) {

            running = false;
            closed = true;

            if (lifecycle_thread != null) {
                lifecycle_thread.interrupt();
            }

            cleanup_mic();

            audio_queue.clear();

            System.out.println("[MicMgr] Microphone manager shut down");
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
    // Utility
    // ============================================================

    private static void safe_sleep(long millis) {

        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
