package com.pasystem;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.pasystem.GlobalSoundQueue;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.nio.ByteBuffer;


@Component
public class SpeakWS extends AbstractWebSocketHandler {
    private ArrayList<WebSocketSession> sessions = new ArrayList<>();
	
	private static byte[] to_bytes(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.slice().get(data);
		return data;
	}
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[SpeakWS] New client connected: " + session);
        this.sessions.add(session);

    }

	@Override
	public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
		GlobalSoundQueue.add(to_bytes(message.getPayload()));
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {
		String data = message.getPayload();
		
		// (0,7) is the index of __IPS__ in a String (if "__IPS__" in data)
		if (data.substring(0,7).equals("__IPS__")) {
			System.out.println("[SpeakWS] Sending new list of IPs");
			GlobalSoundQueue.set_ip_list(data);
		}
	}

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[SpeakWS] Client disconnected: " + session);
        this.sessions.remove(session);
		
    }
}
