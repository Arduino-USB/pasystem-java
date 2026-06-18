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
        // Track states so we only trigger the command once per disconnect event
        boolean speakerWasOnline = true;
        boolean micWasOnline = true;

        while (running) {
            boolean speakerFound = false;
            boolean micFound = false;

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

            // 3. Handle State Changes & Disconnects
            if (!speakerFound && speakerWasOnline) {
                System.out.println("[SoundWatchdog] Alert: Speaker '" + targetSpk + "' has disconnected!");
                trigger_alert();
            }
            
            if (!micFound && micWasOnline) {
                System.out.println("[SoundWatchdog] Alert: Microphone '" + targetMic + "' has disconnected!");
                trigger_alert();
            }

            // Update tracked history
            speakerWasOnline = speakerFound;
            micWasOnline = micFound;

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
