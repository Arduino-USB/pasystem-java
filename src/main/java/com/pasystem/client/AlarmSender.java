package com.pasystem.client;

import java.util.HashMap;
import java.util.Map;

public class AlarmSender {

    public AlarmSender() {
        HTTPWrapper http = new HTTPWrapper();

        // Background thread to continuously monitor the ALARM button
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(50); // Poll frequently to catch presses
                } catch (Exception e) {
                    System.out.println("[AlarmSender] Sleep error: " + e);
                }

                // Check if the "ALARM" button was pressed (triggers once per down action)
                if (GPIOMgr.is_pressed("ALARM")) {
                    
                    // Verify if connected to a server using ConnectionLoop details
                    if (ConnectionLoop.connected()) {
                        String ip = ConnectionLoop.get_ip();
                        int port = ConnectionLoop.get_port();
                        String url = "http://" + ip + ":" + port + "/client_side_alert_trip";

                        // Construct JSON payload: {"alert" : "alarm", "enabled" : true}
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("alert", "alarm");
                        payload.put("enabled", true);

                        try {
                            // Send POST request using HTTPWrapper
                            http.post(url, payload, Map.class);
                            System.out.println("[AlarmSender] Alarm alert sent successfully.");
                        } catch (Exception e) {
                            System.out.println("[AlarmSender] Failed to send alarm request: " + e);
                        }
                    } else {
                        System.out.println("[AlarmSender] ALARM pressed, but no server is currently connected.");
                    }
                }
            }
        }).start();
    }
}
