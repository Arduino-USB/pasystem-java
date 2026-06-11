package com.pasystem;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;


import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.stereotype.Component;

import com.pasystem.FakeClock;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Map;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;



@Component
public class AudioIOWS extends AbstractWebSocketHandler {
    private ArrayList<WebSocketSession> sessions = new ArrayList();
    public Queue<byte[]> queue = new LinkedList<>();
    private WebSocketSession session_get_stream_lock = null;
    private HashMap<String,Long> speaking_users = new HashMap<>();
    private Object lock = new Object();
    private FakeClock clock = new FakeClock();

    public AudioIOWS(){
        System.out.println("[AudioIOWS] Starting speaking_users clean daemon");
        try {
            new Thread(() -> {
                int grace = 1000;
                while (true) {
                    try {Thread.sleep(100);} catch (Exception e) {
                        System.out.println("[AudioIOWS] There was a problem sleeping, " + e);
                    }
                    synchronized (lock) {
                        String[] speaking_users_keys = speaking_users.keySet().toArray(new String[0]);
                        long real_timestamp = clock.get_real_timestamp();
                        for (int i = 0; i < speaking_users_keys.length; i++) {
                            if ((real_timestamp - (long) speaking_users.get(speaking_users_keys[i])) > grace) {
                                System.out.println("[AudioIOWS] The user '" + speaking_users_keys[i] + "' was been queit longer than the grace period. Removing");
                                System.out.println("[AudioIOWS] [DEBUG] REAL: '" + real_timestamp + "', LAST : '" + speaking_users.get(speaking_users_keys[i]) + "'");
                                speaking_users.remove(speaking_users_keys[i]);
                            }
                        }
                    }

                }
            }).start();
        } catch (Exception e) {
            System.out.println("[AudioIOWS] speaking_users clean daemon crashed, " + e);
        }
    }


    private boolean check_loopback_address(WebSocketSession session) {
        InetSocketAddress remote = session.getRemoteAddress();

        if (remote == null) {
            return false;
        }

        InetAddress address = remote.getAddress();

        return address != null && address.isLoopbackAddress();
    }

	private static byte[] to_bytes(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.slice().get(data);
		return data;
	}
	
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[AudioIOWS] New client connected: " + session);
        this.sessions.add(session);
    }

    @Override
	public void handleTextMessage(WebSocketSession session, TextMessage message){
        if (check_loopback_address(session)) {
            if (message.getPayload().equals("GET_AUDIO_STREAM")){
                if (session_get_stream_lock == null) {
                    System.out.println("[AudioIOWS] Registering client " + session + " as the audio stream lock");
                    session_get_stream_lock = session;
                    System.out.println("[AudioIOWS] Starting server audio stream thread");
                    try {
                        
                        new Thread(() -> {
                            while (session.isOpen()){
                                try {Thread.sleep(20);} catch (Exception e) {
                                    System.out.println("[AudioIOWS] There was a problem sleeping, " + e);
                                }

                                synchronized (lock) {
                                    
                                    byte[] chunk = queue.poll();

                                    if (chunk == null) { 
                                        continue;
                                    }

                                    try {session.sendMessage(new BinaryMessage(chunk));} catch (Exception e) {
                                        System.out.println("[AudioIOWS] WS error (audio thread), " + e);
                                    } 
                                    
                                }
                            }
                        }).start();


                        new Thread(() -> {
                            while (session.isOpen()){
                                try {Thread.sleep(500);} catch (Exception e) {
                                    System.out.println("[AudioIOWS] There was a problem sleeping, " + e);
                                }

                                synchronized (lock) {
                                    
                                    if (speaking_users.isEmpty()) {
                                        continue;
                                    }

                                    String[] speaking_users_keys = speaking_users.keySet().toArray(new String[0]);
                                    try {session.sendMessage(new TextMessage (new ObjectMapper().writeValueAsString(speaking_users_keys)));} catch (Exception e) {
                                            System.out.println("[AudioIOWS] WS error (ip thread), " + e);
                                    } 
                                    
                                }
                            }

                        }).start();
                    } catch (Exception e) {
                        System.out.println("[AudioIOWS] Server audio threads crashed, " + e);
                    }

                } else {
                    System.out.println("[AudioIOWS] " + session + "attempted to become to audio stream lock, there already is one");
                }
            }
        } else {
            System.out.println("[AudioIOWS] An IP that isnt local (external) tried accessing the audio stream");
        }

	}

	@Override
	public void handleBinaryMessage(WebSocketSession session, BinaryMessage message){
        String ip = session.getRemoteAddress().getAddress().getHostAddress();
        long timestamp = clock.get_real_timestamp();

        synchronized (lock) {

            if (session_get_stream_lock != null) {
                
                queue.add(to_bytes(message.getPayload()));

                System.out.println("[AudioIOWS] User '" + ip + "' spoke @ " + timestamp);
                speaking_users.put(ip, timestamp);
            
            }
        }

	}

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[AudioIOWS] Client disconnected: " + session);
        this.sessions.remove(session);
    }
}
