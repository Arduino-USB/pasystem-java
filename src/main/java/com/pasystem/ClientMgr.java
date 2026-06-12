package com.pasystem;
import java.util.ArrayList;

import java.util.Map;

import com.pasystem.HTTPWrapper;

public class ClientMgr {
    private ArrayList<Map<String, String>> clients = new ArrayList<>();
    private Thread  alive_checker_thread = new Thread(() -> alive_checker());
    private HTTPWrapper requests = new HTTPWrapper();
    private int CLIENT_PORT = 6742;

    public ClientMgr(){
        System.out.println("[ClientMgr] Starting");
        alive_checker_thread.start();
    }
    public void add_client(String ip){
        System.out.println("[add_client] New IP " + ip + " connected");

        String uname = "UNKNOWN";

        try {
            Map<String, Object> response =
                    requests.get("http://" + ip + ":" + CLIENT_PORT + "/get_settings", Map.class);

            Object name = response.get("name"); // or "response" depending on API

            if (name != null) {
                uname = name.toString();
            }

        } catch (Exception e) {
            System.out.println("[add_client] Couldn't get settings for '" + ip + "'");
            System.out.println("[add_client] Error: " + e);
        }

        clients.add(Map.of("ip", ip, "name", uname));
    }

    public ArrayList<Map<String,String>> get_clients() {
        return clients;
    }

    public String get_user_from_ip(String ip) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).get("ip").equals(ip)) {
                return clients.get(i).get("name");
            }
        }

        System.out.println("[get_user_from_ip] Could not find the username for '" + ip +"' in connected clients");
        return "UNKNOWN";
    }

    private void alive_checker() {
        System.out.println("[alive_checker] Starting alive_checker");

        while (true) {
            try {
                Thread.sleep(1000);

                for (int i = clients.size() - 1; i >= 0; i--) {
                    try {
                        String ip = clients.get(i).get("ip");

                        requests.get(
                            "http://" + ip + ":" + CLIENT_PORT + "/alive",
                            Map.class
                        );

                    } catch (Exception e) {
                        System.out.println("[alive_checker] Removing dead client: " + clients.get(i));
                        clients.remove(i);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}