package com.pasystem.client;

import com.pasystem.client.SpeakerMgr;

import java.util.List;

public class SoundWatchdog {
    private static boolean instance_created = false;
    
    private static String targetSpk;
    private static String targetMic;
    private static String alertCommand;
    private static volatile boolean running = true;
    private static final Object lock = new Object();

    // Track when the alert command was last triggered for each device to handle 10-second re-runs
    private static long speakerLastAlertTime = 0;
    private static long micLastAlertTime = 0;
    private static final long RETRY_INTERVAL_MS = 10000; // 10 seconds

    // Constructor acts as the one-time static initializer
    public SoundWatchdog(String target_spk, String target_mic, String command) {
        synchronized (lock) {
            if (instance_created) {
                System.out.println("[SoundWatchdog] There can only be one instance of SoundWatchdog, and then it is meant to be used as a static utility.");
                return;
            }
            targetSpk = target_spk;
            targetMic = target_mic;
            alertCommand = command;
            instance_created = true;

            // Start the monitoring watchdog thread
            new Thread(SoundWatchdog::monitor_devices, "SoundWatchdog-Loop").start();
            System.out.println("[SoundWatchdog] Monitoring started for Speaker: '" + targetSpk + "' and Mic: '" + targetMic + "'");
        }
    }

    private static void monitor_devices() {
        // Track states so we can detect initial disconnects vs continuous offline status
        boolean speakerWasOnline = true;
        boolean micWasOnline = true;

        while (running) {
            boolean speakerFound = false;
            boolean micFound = false;
            long currentTime = System.currentTimeMillis();

            // 1. Check Speakers
            List<String> activeSpeakers = SpeakerMgr.get_speakers();
            for (String spk : activeSpeakers) {
                if (spk.contains(targetSpk)) {
                    speakerFound = true;
                    break;
                }
            }

            // 2. Check Mics
            List<String> activeMics = MicMgr.get_mics();
            for (String mic : activeMics) {
                if (mic.contains(targetMic)) {
                    micFound = true;
                    break;
                }
            }

            // 3. Handle Speaker State & Re-trigger logic
            if (!speakerFound) {
                if (speakerWasOnline) {
                    System.out.println("[SoundWatchdog] Alert: Speaker '" + targetSpk + "' has disconnected!");
                    trigger_alert();
                    speakerLastAlertTime = currentTime;
                    speakerWasOnline = false;
                } else if (currentTime - speakerLastAlertTime >= RETRY_INTERVAL_MS) {
                    System.out.println("[SoundWatchdog] Reminder: Speaker '" + targetSpk + "' is still offline. Re-running alert command...");
                    trigger_alert();
                    speakerLastAlertTime = currentTime;
                }
            } else {
                speakerWasOnline = true;
            }

            // 4. Handle Mic State & Re-trigger logic
            if (!micFound) {
                if (micWasOnline) {
                    System.out.println("[SoundWatchdog] Alert: Microphone '" + targetMic + "' has disconnected!");
                    trigger_alert();
                    micLastAlertTime = currentTime;
                    micWasOnline = false;
                } else if (currentTime - micLastAlertTime >= RETRY_INTERVAL_MS) {
                    System.out.println("[SoundWatchdog] Reminder: Microphone '" + targetMic + "' is still offline. Re-running alert command...");
                    trigger_alert();
                    micLastAlertTime = currentTime;
                }
            } else {
                micWasOnline = true;
            }

            // Sleep quietly before checking again (e.g., every 2.5 seconds)
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void trigger_alert() {
        try {
            String output = CmdExec.run(alertCommand);
            System.out.println("[SoundWatchdog] Command returned: " + output);
        } catch (Exception e) {
            System.out.println("[SoundWatchdog] Failed to execute alert command: " + e.getMessage());
        }
    }

    /**
     * Stops the watchdog monitoring loop.
     */
    public static void stop() {
        running = false;
    }
}
