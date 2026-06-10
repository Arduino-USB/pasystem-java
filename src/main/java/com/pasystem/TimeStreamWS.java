package com.pasystem;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.pasystem.FakeClock;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Component
public class TimeStreamWS extends AbstractWebSocketHandler {
    private ArrayList<WebSocketSession> sessions = new ArrayList();
    private FakeClock clock = new FakeClock();

    private static byte[] to_bytes(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.slice().get(data);
		return data;
	}

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[TimeStreamWS] New client connected: " + session);
        this.sessions.add(session);

        try{
            new Thread(() -> {
                System.out.println("[TimeStreamWS] Sending WS data");
                while (session.isOpen()) {
                    try {Thread.sleep(500);} catch (Exception e){
                        System.out.println("[TimeStreamWS] Sleep error, " + e);
                    }
                    
                    try{session.sendMessage(new TextMessage(clock.get_time()));} catch (Exception e){
                        System.out.println("[TimeStreamWS] WS send error, " + e);
                    }
                    
                }

                System.out.println("[TimeStreamWS] Exiting thread (client probably disconnected)");
                
            }).start();
        } catch (Exception e) {
            System.out.println("[TimeStreamWS] Error occoured ");
            System.out.println(e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[TimeStreamWS] Client disconnected: " + session);
        this.sessions.remove(session);
    }
}
