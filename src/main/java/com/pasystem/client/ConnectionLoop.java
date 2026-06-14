package com.pasystem.client;

import com.pasystem.client.PortScanner;
import com.pasystem.client.HTTPWrapper;

import java.util.ArrayList;
import java.util.Map;

public class ConnectionLoop {
	private static boolean instance_created = false;
	private static boolean connected = false;
	private static String connected_ip = null;

	public ConnectionLoop (){
		
		PortScanner port_scanner = new PortScanner();
		HTTPWrapper requests = new HTTPWrapper();
		
		if (instance_created) {
			System.out.println("[ConnectionLoop] Only one ConnectionLoop instance is allowed, then it is meant to be used statically");
			return;
		}
		
			
		System.out.println("[ConnectionLoop] Starting connection daemon");
		
		new Thread(() -> {
			while (true) {
				
				try {Thread.sleep(1000);} catch (Exception e) {
					System.out.println("[ConnectionLoop] Sleep error, " + e);
				}	
			
				if (!connected) {
					System.out.println("[ConnectionLoop] Searching for server");
					ArrayList<String> potential_servers = port_scanner.scan_network(6741);
					
					if (potential_servers.size() == 0 ) {
						System.out.println("[ConnectionLoop] No servers found, trying again");
						continue;
					} else if (potential_servers.size() > 1) {
						System.out.println("[ConnectionLoop] There is more than one server, waiting for there to only be one");
						continue;
					} else if (potential_servers.size() == 1) {
						System.out.println("[ConnectionLoop] Server found");
						connected = true;	
						connected_ip = potential_servers.get(0);	
					}	
				} else {
					
					try {
						requests.get("http://" + connected_ip + ":" + 6741 + "/register", Map.class);
						System.out.println("[ConnectionLoop] Registered existance to server");
						while (true) {
							try {Thread.sleep(1000);} catch (Exception e) {
								System.out.println("[ConnectionLoop] Sleep error, " + e);
							}
							
							requests.get("http://" + connected_ip + ":" + 6741 + "/alive", Map.class);
						}
					} catch (Exception e) {
						System.out.println("[ConnectionLoop] Server disconnected, " + e);
						connected = false;
						connected_ip = null;	
					}
				}
				
				
				
			}	
		}).start();
			
		instance_created = true;

	}
	
		
	public static String get_ip() {
		return connected_ip;	
	}
	
	public static boolean connected() {
		return connected;	
	}
}
