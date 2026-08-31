package com.pasystem.client;

import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClockDisplay {

    private static String lastSavedTime = "";
    private static boolean instanceCreated = false;
    
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh-mm-a", Locale.US);

    public ClockDisplay() {
        if (instanceCreated) {
            System.out.println("[ClockDisplay] Only one ClockDisplay instance allowed then it is meant to be used as a static utility");
            return;
        }    
        instanceCreated = true;

        new Thread(() -> {            
            Path fontPath = Path.of("font.ttf");
            int alertX = 0; // X percentage position
            int alertY = 0; // Y percentage position

            System.out.println("[ClockDisplay] Background thread started successfully.");

            while (true) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    System.out.println("[ClockDisplay] Sleep error, " + e);    
                }
                
                // --- ALERT STAGE ---
                List<String> activeAlerts = AlertMgrClient.get_alerts();
                
                if (activeAlerts != null && !activeAlerts.isEmpty()) {
                    System.out.println("[ClockDisplay] >>> ALERTS DETECTED: " + activeAlerts);
                    
                    // Clear screen once before entering the alert flashing phase
                    RenderMgr.clearScreen(); 

                    // Cache SixelImages in an ArrayList once per alert trigger
                    List<CachedAlertImages> cachedAlertsList = new ArrayList<>();
                    for (String alertText : activeAlerts) {
                        System.out.println("[ClockDisplay] Caching images for alert text: '" + alertText + "'");
                        SixelImage blackImg = new SixelImage(alertText.toUpperCase(), "#000000", "#FF0000", fontPath, 0, 720, 400);
                        SixelImage whiteImg = new SixelImage(alertText.toUpperCase(), "#FFFFFF", "#FF0000", fontPath, 0, 720, 400);
                        cachedAlertsList.add(new CachedAlertImages(blackImg, whiteImg));
                    }

                    System.out.println("[ClockDisplay] Entering alert flashing loop with " + cachedAlertsList.size() + " cached alert(s).");

                    // Flashing loop
                    alertLoop:
                    while (true) {
                        // Check if alerts have cleared globally before starting a full sequence pass
                        activeAlerts = AlertMgrClient.get_alerts();
                        if (activeAlerts == null || activeAlerts.isEmpty()) {
                            System.out.println("[ClockDisplay] [Loop Check] Alerts cleared globally at top of loop. Breaking alertLoop.");
                            break alertLoop;
                        }

                        for (int i = 0; i < cachedAlertsList.size(); i++) {
                            CachedAlertImages cachedAlert = cachedAlertsList.get(i);
                            
                            // --- CHECK BEFORE BLACK FRAME ---
                            List<String> checkBeforeBlack = AlertMgrClient.get_alerts();
                            System.out.println("[ClockDisplay] [Check Pre-Black] AlertMgrClient.get_alerts() returned: " + checkBeforeBlack);
                            if (checkBeforeBlack == null || checkBeforeBlack.isEmpty()) {
                                System.out.println("[ClockDisplay] [Check Pre-Black] Alerts are empty/null. Breaking alertLoop.");
                                break alertLoop;
                            }

                            // 1. Display BLACK background frame
                            System.out.println("[ClockDisplay] Displaying BLACK frame for alert index " + i);
                            RenderMgr.clearScreen();
                            RenderMgr.renderImageNoHistory(cachedAlert.blackImg, alertX, alertY);
                            
                            try { 
                                Thread.sleep(500); 
                            } catch (InterruptedException e) { 
                                System.out.println("[ClockDisplay] Interrupted during black frame sleep.");
                                Thread.currentThread().interrupt(); 
                                break alertLoop; 
                            }

                            // --- CHECK BEFORE WHITE FRAME ---
                            List<String> checkBeforeWhite = AlertMgrClient.get_alerts();
                            System.out.println("[ClockDisplay] [Check Pre-White] AlertMgrClient.get_alerts() returned: " + checkBeforeWhite);
                            if (checkBeforeWhite == null || checkBeforeWhite.isEmpty()) {
                                System.out.println("[ClockDisplay] [Check Pre-White] Alerts are empty/null. Breaking alertLoop.");
                                break alertLoop;
                            }

                            // 2. Display WHITE background frame
                            System.out.println("[ClockDisplay] Displaying WHITE frame for alert index " + i);
                            RenderMgr.clearScreen();
                            RenderMgr.renderImageNoHistory(cachedAlert.whiteImg, alertX, alertY);
                            
                            try { 
                                Thread.sleep(500); 
                            } catch (InterruptedException e) { 
                                System.out.println("[ClockDisplay] Interrupted during white frame sleep.");
                                Thread.currentThread().interrupt(); 
                                break alertLoop; 
                            }
                        }
                    }

                    System.out.println("[ClockDisplay] <<< Exited alert flashing loop. Cleaning up screen and resetting clock.");
                    // Clear screen once after alerts finish, reset clock timer to force immediate refresh
                    RenderMgr.clearScreen();
                    lastSavedTime = "";
                    continue; 
                }

                // --- NORMAL CLOCK MODE ---
                String currentTime = FakeClock.getTimeAsInstant()
                    .atZone(ZoneOffset.UTC)
                    .format(formatter);
        
                if (!currentTime.equals(lastSavedTime)) {
                    System.out.println("[ClockDisplay] Time updated, changing clock face to: " + currentTime);
                    RenderMgr.clearScreen();
                    RenderMgr.renderImage(Path.of("screenImages/clockFaces", currentTime + ".txt"), 0, 0);
                    lastSavedTime = currentTime;
                }
            }            
        }).start();    
    }

    // Helper holder class for cached images
    private static class CachedAlertImages {
        SixelImage blackImg;
        SixelImage whiteImg;

        public CachedAlertImages(SixelImage blackImg, SixelImage whiteImg) {
            this.blackImg = blackImg;
            this.whiteImg = whiteImg;
        }
    }
}
