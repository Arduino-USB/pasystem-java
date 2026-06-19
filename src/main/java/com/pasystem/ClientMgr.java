package com.pasystem;

import java.util.ArrayList;
import java.util.Map;
import com.pasystem.HTTPWrapper;

public class ClientMgr {
    private ArrayList<Map<String, String>> clients = new ArrayList<>();
    private Thread alive_checker_thread = new Thread(() -> alive_checker());
    private HTTPWrapper requests = new HTTPWrapper();
    private int CLIENT_PORT = 6742;

    public ClientMgr(){
        System.out.println("[ClientMgr] Starting");
        alive_checker_thread.start();
    }

    public synchronized void add_client(String ip){
        // Prevent duplicate IP registrations
        for (Map<String, String> client : clients) {
            if (client.get("ip").equals(ip)) {
                return; 
            }
        }

        System.out.println("[ClientMgr] New IP " + ip + " connected");
        String uname = fetch_latest_name(ip);
        clients.add(Map.of("ip", ip, "name", uname));
    }

    public synchronized ArrayList<Map<String,String>> get_clients() {
        // Return a copy to prevent ConcurrentModificationExceptions if the thread modifies it during an API read
        return new ArrayList<>(clients);
    }

    public synchronized String get_user_from_ip(String ip) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).get("ip").equals(ip)) {
                return clients.get(i).get("name");
            }
        }
        System.out.println("[ClientMgr] Could not find the username for '" + ip +"' in connected clients");
        return "UNKNOWN";
    }

    // Helper method to fetch the name reliably from the device settings
    private String fetch_latest_name(String ip) {
        try {
            Map<String, Object> response = requests.get("http://" + ip + ":" + CLIENT_PORT + "/get_settings", Map.class);
            if (response != null && response.get("name") != null) {
                return response.get("name").toString();
            }
        } catch (Exception e) {
            System.out.println("[ClientMgr] Warning: Couldn't get settings name profile for '" + ip + "'");
        }
        return "UNKNOWN";
    }

    private void alive_checker() {
        System.out.println("[ClientMgr] Starting alive_checker loop");

        while (true) {
            try {
                Thread.sleep(1000);

                // Use synchronized block to prevent race conditions during list updates
                synchronized (this) {
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        Map<String, String> currentClient = clients.get(i);
                        String ip = currentClient.get("ip");
                        String oldName = currentClient.get("name");

                        try {
                            // 1. Confirm device network status is still online
                            requests.get("http://" + ip + ":" + CLIENT_PORT + "/alive", Map.class);

                            // 2. Fetch its current configured name
                            String currentName = fetch_latest_name(ip);

                            // 3. If the user edited their name, update the internal array entry
                            if (!oldName.equals(currentName)) {
                                System.out.println("[ClientMgr] Updating name for " + ip + " from '" + oldName + "' to '" + currentName + "'");
                                clients.set(i, Map.of("ip", ip, "name", currentName));
                            }

                        } catch (Exception e) {
                            // Device timed out or went offline
                            System.out.println("[ClientMgr] Removing dead client: " + currentClient);
                            clients.remove(i);
                        }
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
