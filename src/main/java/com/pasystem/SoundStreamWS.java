package com.pasystem;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.pasystem.GlobalSoundQueue;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;


@Component
public class SoundStreamWS extends AbstractWebSocketHandler {
    private ArrayList<WebSocketSession> sessions = new ArrayList();

    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[SoundStreamWS] New client connected: " + session);
        this.sessions.add(session);

        try{
            new Thread(() -> {
                System.out.println("[SoundStreamWS] Sending sound data");
                while (session.isOpen()) {
                    try {Thread.sleep(20);} catch (Exception e){
                        System.out.println("[SoundStreamWS] Sleep error, " + e);
                    }
                    

					if (GlobalSoundQueue.ip_list_changed()) {
						System.out.println("[SoundStreamWS] IP list changed, sending it");
						try{session.sendMessage(new TextMessage("__IPS__" + GlobalSoundQueue.get_ip_list()));} catch (Exception e){
		                    System.out.println("[SoundStreamWS] WS send error, " + e);
		                }	
					}

					byte[] next_byte = GlobalSoundQueue.poll(session);
					
					if (next_byte != null){
		                try{session.sendMessage(new BinaryMessage(next_byte));} catch (Exception e){
		                    System.out.println("[SoundStreamWS] WS send error, " + e);
		                }
					}
                    
                }

                System.out.println("[SoundStreamWS] Client disconnected)");
                
            }).start();

        } catch (Exception e) {
            System.out.println("[SoundStreamWS] Error occoured " + e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[SoundStreamWS] Client disconnected: " + session);
        this.sessions.remove(session);
		GlobalSoundQueue.remove_session(session);
    }
}
