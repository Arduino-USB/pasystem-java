package com.pasystem.client;

import com.pasystem.client.NetworkMgr;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PortScanner {

	private static final ExecutorService executor = Executors.newFixedThreadPool(100);
	public ArrayList<String> scan_network(int port) {
		
		Map<String,Future<Boolean>> futures = new HashMap<>();
		String ip = NetworkMgr.get_ip();
		int total_ips = 1 << (32 - NetworkMgr.get_subnet());
		

		String ip_part = ip.substring(0, ip.lastIndexOf(".") + 1);
		System.out.println("[PortScanner] Scanning " + total_ips + " ips on type " + ip_part + "X for open port " + port);
		
		for (int i = 0; i < total_ips; i++) {
			String current_ip = ip_part + i;
			futures.put(current_ip, executor.submit(() -> {
				return scan_ip(current_ip, port);	
			}));	
		}
		
		String[] futures_keys = futures.keySet().toArray(new String[0]);
		
		ArrayList<String> ips = new ArrayList<>();
		for (int i = 0; i < futures_keys.length; i++) {
			String current_ip = futures_keys[i];
			try {
				if (futures.get(current_ip).get()) {
					ips.add(current_ip);	
				}
			} catch (Exception e){
				System.out.println("[PortScanner] Error getting return value, " + e);	
			}
		}
		
		
		return ips;

		
		
	}

	public static boolean scan_ip(String host, int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 300);
			return true;
		} catch (Exception e) {
			return false;
		}
	}


}


