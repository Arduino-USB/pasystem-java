package com.pasystem.client;

import com.pasystem.client.SpeakerMgr;
import java.io.FileInputStream;
import javax.sound.sampled.Clip;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.nio.file.Path;

public class AlertMgrClient {
    
    private static String path = ""; 

    private static class SoundAlert {
        private Clip clip;
        private String file_path = null;
        private boolean playing = false; 
        private String alert = null;

        public SoundAlert(String alert) {
            this.alert = alert;
            System.out.println("[SoundAlert] Searching for sound file for alert '" + alert + "'");
            
            List<String> possible_paths = List.of(alert + ".wav", Path.of(path, alert + ".wav").toString());
            for (String p : possible_paths) {    
                System.out.println("[SoundAlert] Checking for sound file for alert '" + alert + "' in filepath '" + p + "'");
                if (new File(p).exists()) {
                    file_path = p;
                    System.out.println("[SoundAlert] Found file in path '" + p + "'");
                    break;
                }
            }         
            
            if (file_path == null) {
                System.out.println("[SoundAlert] Could not find alert sound for '" + alert + "'");
                clip = null;
                start();
                return;   
            }
    
            // Just initialize the clip reference without double-playing in the constructor
            try (FileInputStream file_stream = new FileInputStream(new File(file_path))) {
                clip = SpeakerMgr.play_file(file_stream);
            } catch (Exception e) {    
                System.out.println("\n[SoundAlert] Error initializing clip stream for '" + alert + "'");
                e.printStackTrace();
            }
            
            start();
        }
        
        public synchronized void start() {
            try {
                if (file_path == null) {
                    System.out.println("[SoundAlert] Playing emergency buzzer (no file) '" + alert + "'");
                    playing = true;
                    return;
                }

                if (clip == null || !clip.isOpen()) {
                    System.out.println("[SoundAlert] Re-acquiring clip for '" + alert + "'");
                    try (FileInputStream file_stream = new FileInputStream(new File(file_path))) {
                        clip = SpeakerMgr.play_file(file_stream);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (clip != null) {
                    clip.stop(); // Stop if it was running
                    clip.setFramePosition(0); // CRITICAL: Reset playhead to the beginning
                    clip.setMicrosecondPosition(0);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    playing = true;
                    System.out.println("[SoundAlert] Successfully started looping alert '" + alert + "'");
                } else {
                    System.out.println("[SoundAlert] Speaker is down, running in fallback mode for '" + alert + "'");
                    playing = true;
                }

            } catch (Exception e) {
                System.out.println("[SoundAlert] Unexpected error while starting '" + alert + "'");
                e.printStackTrace();
            }
        }

        public synchronized void stop() {
            if (!playing) {
                return;
            }

            if (clip == null) {
                System.out.println("[SoundAlert] Stopping emergency buzzer '" + alert + "'");
                playing = false;
                return;
            }

            try {
                if (clip.isOpen()) {
                    clip.stop();
                }
            } catch (Exception ignored) {
            }
            
            playing = false;
            System.out.println("[SoundAlert] Stopped alert '" + alert + "'");
        }
            
        public boolean playing() {
            return playing;            
        }
    }

    private static Map<String, SoundAlert> alerts = new HashMap<>();

    public static synchronized void set_alert(String alert) {
        if (alerts.containsKey(alert)) {
            if (alerts.get(alert).playing()) {
                System.out.println("[AlertMgrClient] Cannot play alert '" + alert + "' as it is already playing");
                return;
            }
            
            System.out.println("[AlertMgrClient] Playing existing alert instance '" + alert + "'");
            alerts.get(alert).start();
            return;
        }
        
        System.out.println("[AlertMgrClient] Adding + playing new alert '" + alert + "'");
        alerts.put(alert, new SoundAlert(alert));
    }

    public static synchronized void stop_alert(String alert) {
        if (!alerts.containsKey(alert)) {    
            System.out.println("[AlertMgrClient] The alert '" + alert + "' cannot be stopped because it doesn't exist");
            return;
        }
        
        if (!alerts.get(alert).playing()) {
            System.out.println("[AlertMgrClient] The alert '" + alert + "' cannot be stopped because it isn't playing");
            return;        
        }
        
        System.out.println("[AlertMgrClient] Stopping alert '" + alert + "'");
        alerts.get(alert).stop();
        alerts.remove(alert);
    }
    
    public static List<String> get_alerts() {
        return List.of(alerts.keySet().toArray(new String[0]));        
    }
}
