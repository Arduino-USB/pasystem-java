package com.pasystem.client;

import com.pasystem.client.ConnectionLoop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;

import java.util.HashMap;
import java.util.Map;

public class WSConnectionMgr {
	
	private static final WebSocket.Builder ws_builder = HttpClient.newHttpClient().newWebSocketBuilder();
	private Map<String, WebSocket> connections = new HashMap<>();
	private Map<String, WebSocket.Listener> endpoints_handlers_map; 	

	public WSConnectionMgr(Map<String, WebSocket.Listener> endpoints_listeners) {
		endpoints_handlers_map = endpoints_listeners;
		String[] endpoints = endpoints_handlers_map.keySet().toArray(new String[0]);
		new Thread(() -> {	
			while (true) {
				
				try {Thread.sleep(1000);} catch (Exception e) {
					System.out.println("[WSConnectionMgr] Error sleeping, " + e);
				}
				
				if (ConnectionLoop.connected()) {
					
					for (int i = 0; i < endpoints.length; i++) {
						try {
							
							if (connections.containsKey(endpoints[i])) {
								WebSocket connection = connections.get(endpoints[i]);
								if (connection.isInputClosed() || connection.isOutputClosed()) {
									System.out.println("[WSConnectionMgr] The endpoint '" + endpoints[i] + "' is down. Re-establishing");
									connection.abort();
	
								} else {
									continue;
								}	
							}
 
							connections.put(
								endpoints[i], 
								ws_builder.buildAsync(
									URI.create("ws://" + ConnectionLoop.get_ip() + ":" + ConnectionLoop.get_port() + endpoints[i]), 
									endpoints_handlers_map.get(endpoints[i])
								).join()
							);
							
						} catch (Exception e) {
							System.out.println("[WSConnectionMgr] Error connecting to WS endpoint '" + endpoints[i] + "', " + e);
						}

					}
					
				}	
			}
		}).start();	
	}
		
		
	public void print_status() {
		String[] connections_keys = connections.keySet().toArray(new String[0]);
		System.out.println("[WSConnectionMgr] All WS status:");
		for (int i = 0; i < connections_keys.length; i++) {
			System.out.println("\t\t" + connections_keys[i] + ": In (closed): " + connections.get(connections_keys[i]).isInputClosed() + ", Out (closed): " + connections.get(connections_keys[i]).isOutputClosed());
		}                    	
			                 	
	}	                     	
//	while ()	             	
//	ws_builder.buildAsync(URI.create("ws:///ws"), new AudioRecvWS()).join()
	
}
