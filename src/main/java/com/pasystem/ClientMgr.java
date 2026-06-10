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
        try{uname = (String) requests.get("http://" + ip + ":" + CLIENT_PORT + "/get_settings").get("name");} catch (Exception e)  {
            System.out.println("[add_client] Couldn't get settings for '" + ip + "', setting name to UNKNOWN");
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

                for (int i = 0; i < clients.size(); i++) {
                    try {
                        requests.get(
                            "http://" + clients.get(i).get("ip")
                            + ":" 
                            + CLIENT_PORT
                            + "/alive"
                        );
                    } catch (Exception e) {
                        System.out.println(
                            "[alive_checker] There seems to be an issue making the /alive request"
                        );
                        clients.remove(i);
                        //System.out.println(e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}