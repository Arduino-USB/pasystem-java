package com.pasystem;

import org.springframework.web.socket.WebSocketSession;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;


public class GlobalSoundQueue {
	
	private static HashMap<WebSocketSession,Queue<byte[]>> users_queue = new HashMap<>();
	private static String current_ip_list = "";
	private static boolean ip_list_changed = false;
	private static final int MAX_QUEUE_SIZE = 50;

	public static synchronized void add(byte[] sound_byte) {
	
		// every clients queue
		for (Queue<byte[]> queue : users_queue.values()) {
			if (queue.size() >= MAX_QUEUE_SIZE) {
				queue.poll(); // remove oldest
			}
			queue.add(sound_byte.clone());
		}
	}

	public static synchronized byte[] poll(WebSocketSession session) {
		
		if (users_queue.containsKey(session) != true){
			System.out.println("[GlobalSoundQueue] New client connected to per client Queue system");
			users_queue.put(session, new LinkedList<>());			
		} 
			
		return users_queue.get(session).poll();
		
	}
	
	public static synchronized void remove_session(WebSocketSession session) {
		System.out.println("[GlobalSoundQueue] Client disconnected from per client Queue system");
		users_queue.remove(session);
	}

	public static void set_ip_list(String ip_list) {
		current_ip_list = ip_list;
		ip_list_changed = true;
	}
	
	public static String get_ip_list() {
		ip_list_changed = false;
		return current_ip_list;
	}
	
	public static boolean ip_list_changed() {	
		return ip_list_changed;
	}
		
}
