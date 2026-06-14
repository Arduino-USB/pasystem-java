package com.pasystem.client;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.InterfaceAddress;


public class NetworkMgr {





    private static boolean instance_created = false;
    public static IfaceInfo selected_interface = null;


    public class IfaceInfo {
        NetworkInterface iface;
        String ip;
        short subnet;
    }

   

    public NetworkMgr() {

        if (instance_created) {
            System.out.println("[NetworkMgr] There can only be one given instance of NetworkMgr, and then it is meant to be used a a static utlity");
            return;
        }        

        System.out.println("[NetworkMgr] Searching for vaild interface");

        Enumeration<NetworkInterface> network_interfaces_enumeration = null;

        try {network_interfaces_enumeration = NetworkInterface.getNetworkInterfaces();} catch (Exception e) {
            System.out.println("[NetworkMgr] Error getting network interfaces, " + e);
        }

        NetworkInterface[] ifaces = Collections.list(network_interfaces_enumeration).toArray(new NetworkInterface[0]);
        ArrayList<IfaceInfo> vaild_interfaces = new ArrayList<>();
        for (int i = 0; i < ifaces.length; i++) {
            
            NetworkInterface current_interface = ifaces[i];
            List<InterfaceAddress> addresses = current_interface.getInterfaceAddresses();


            
            for (int j = 0; j < addresses.size(); j++) {
                InterfaceAddress current_address = addresses.get(j);
                if (valid_ip(current_address.getAddress())) {
                    System.out.println("[NetworkMgr] Interface '" + current_interface.getDisplayName() + "' is a vaild interface with ip '" + current_address.getAddress().toString().substring(1) + "'");

                    IfaceInfo iface_info = new IfaceInfo();
                    iface_info.iface = current_interface;
                    iface_info.ip = current_address.getAddress().toString().substring(1);
                    iface_info.subnet = current_address.getNetworkPrefixLength();

                    vaild_interfaces.add(iface_info);
                }
            }


        }

            if (vaild_interfaces.size() == 1) {
                System.out.println("[NetworkMgr] Selecting the only vaild interface with ip '" + vaild_interfaces.get(0).ip + "'");
                selected_interface = vaild_interfaces.get(0);
                instance_created = true;

            } else if (vaild_interfaces.size() > 1) {
                System.out.println("[NetworkMgr] WARN: There is more than one vaild interface");
                System.out.println("[NetworkMgr] Selecting the first vaild interface with ip '" + vaild_interfaces.get(0).ip + "'");
                selected_interface = vaild_interfaces.get(0);
                instance_created = true;
            } else {
                System.out.println("[NetworkMgr] WARN: There are no vaild interfaces");
            }

    }



    private static boolean valid_ip(InetAddress ip) {
        try {
    
            // Must be a valid IP format (not something unresolved)
            if (ip == null) return false;

            // Reject loopback (127.x.x.x or ::1)
            if (ip.isLoopbackAddress()) return false;

            // Optional: reject link-local (169.254.x.x / fe80::)
            if (ip.isLinkLocalAddress()) return false;

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public static String get_ip() {
        if (selected_interface == null) {
            return null;
        }

        return selected_interface.ip;

    }

    public static Short get_subnet() {
        if (selected_interface == null) {
            return null;
        }

        return selected_interface.subnet;

    }

}
