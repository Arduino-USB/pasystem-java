package com.pasystem;

import java.util.ArrayList;
import java.util.Map;
import com.pasystem.HTTPWrapper;

public class ClientMgr {
    private static ArrayList<Map<String, String>> clients = new ArrayList<>();
    private Thread alive_checker_thread = new Thread(() -> alive_checker());
    private static HTTPWrapper requests = new HTTPWrapper();
    private int CLIENT_PORT = 6742;

    public ClientMgr(){
        System.out.println("[ClientMgr] Starting");
        alive_checker_thread.start();
    }

    public synchronized void add_client(String ip){
        for (Map<String, String> client : clients) {
            if (client.get("ip").equals(ip)) {
                return; 
            }
        }

        System.out.println("[ClientMgr] New IP " + ip + " connected");
        String uname = fetch_latest_name(ip);
        clients.add(Map.of("ip", ip, "name", uname));
    }

    public static synchronized ArrayList<Map<String,String>> get_clients() {
        return new ArrayList<>(clients);
    }

    public static synchronized String get_user_from_ip(String ip) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).get("ip").equals(ip)) {
                return clients.get(i).get("name");
            }
        }
        System.out.println("[ClientMgr] Could not find the username for '" + ip +"' in connected clients");
        return "UNKNOWN";
    }

    private String fetch_latest_name(String ip) {
        try {
            Map response = requests.get("http://" + ip + ":" + CLIENT_PORT + "/get_settings", Map.class);
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

                synchronized (this) {
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        Map<String, String> currentClient = clients.get(i);
                        String ip = currentClient.get("ip");
                        String oldName = currentClient.get("name");

                        try {
                            // 1. Confirm device network status is still online 
                            // (Using Object.class or allowing null return for simple 200 OK responses)
                            requests.get("http://" + ip + ":" + CLIENT_PORT + "/alive", Object.class);

                            // 2. Fetch its current configured name
                            String currentName = fetch_latest_name(ip);

                            // 3. Update if name changed
                            if (!oldName.equals(currentName)) {
                                System.out.println("[ClientMgr] Updating name for " + ip + " from '" + oldName + "' to '" + currentName + "'");
                                clients.set(i, Map.of("ip", ip, "name", currentName));
                            }

                        } catch (Exception e) {
                            // Device timed out, threw an error, or went offline -> Remove it
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
