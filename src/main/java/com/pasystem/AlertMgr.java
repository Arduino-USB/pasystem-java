package com.pasystem;

import java.util.ArrayList;

public class AlertMgr {

    ArrayList<ClientData> clients = new ArrayList<>();

    public static class ClientData {
        public String ip;
        public ArrayList<String> alerts = new ArrayList<>();
    }

    public void set_all_alerts(String ip, ArrayList<String> alerts){
        boolean found = false;
        for (int i = 0; i < clients.size(); i++) {
            ClientData client = clients.get(i);

            if (client.ip.equals(ip)) {
                System.out.println("[AlertMgr] Setting alerts to '" + alerts + "' for exising ip '" + ip + "'");
                client.alerts = alerts;
                found = true;
            }
        
        }

        if (!found) {
            System.out.println("[AlertMgr] Setting alerts to '" + alerts + "' for new ip '" + ip + "'");
            ClientData client = new ClientData();
            client.ip = ip;
            client.alerts = alerts;

            clients.add(client);
        }
    }

    public void set_alert(String ip, String alert) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).ip.equals(ip)) {
                if (clients.get(i).alerts.contains(alert)) {
                    System.out.println("[AlertMgr] Cannot add alert '" + alert + "' to ip '" + ip + "' as it already exists");
                    return;
                }
            }
        }

        ClientData client = new ClientData();
        client.ip = ip;
        client.alerts.add(alert);

        clients.add(client);

    }


    public void remove_alert(String ip, String alert) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).ip.equals(ip)) {
                if (clients.get(i).alerts.contains(alert)) {

                    clients.get(i).alerts.remove(alert);
                    
                } else {
                    System.out.println("[AlertMgr] Cannot remove alert '" + alert + "' from ip '" + ip + "' as it doesnt exists");
                }
            }
        }

        

    }


    public ArrayList<String> get_alerts(String ip){
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).ip.equals(ip)) {
                return clients.get(i).alerts;
            } 
        }

        System.out.println("[AlertMgr] Couldn't find alerts of '" + ip + "' as it doesnt exist. Creating");

        ClientData client = new ClientData();
        client.ip = ip;
        clients.add(client);


        return client.alerts;
    }

    public ArrayList<ClientData> get_all_alerts() {
        return clients;
    }

}
