package com.pasystem;

import java.util.ArrayList;

public class AlertMgr {

    ArrayList<ClientData> clients = new ArrayList<>();

    public class ClientData {
        String ip;
        String status;
    }

    public void set_status(String ip, String status){
        boolean found = false;
        for (int i = 0; clients.size(); i++) {
            ClientData client = clients.get(i);

            if (client.ip.equals(ip)) {
                System.out.println("[AlertMgr] Setting state to '" + "' for exising ip '" + ip + "'");
                client.status = status;
                found = true;
            }
        
        }

        if (!found) {
            System.out.println("[AlertMgr] Setting state to '" + "' for new ip '" + ip + "'");
            ClientData client = new ClientData();
            client.ip = ip;
            client.status = status;

            clients.add(client);
        }
    }

    public String get_status(String ip){
        for (int i = 0; clients.size(); i++) {
            if (clients.get(i).ip.equals(ip)) {
                return clients.get(i).status;
            } 
        }

        System.out.println("[AlertMgr] Couldn't find state of '" + ip + "' as it doesnt exist. Creating");

        ClientData client = new ClientData();
        client.ip = ip;
        clients.add(client);


        return client.status;
    }

}