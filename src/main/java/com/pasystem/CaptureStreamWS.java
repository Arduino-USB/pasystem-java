package com.pasystem;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;


import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.stereotype.Component;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;



@Component
public class CaptureStreamWS extends AbstractWebSocketHandler {
    private ArrayList<WebSocketSession> sessions = new ArrayList();
    private Queue<byte[]> queue = new LinkedList<>();

	private static byte[] to_bytes(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.slice().get(data);
		return data;
	}
	
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[CaptureStreamWS] New client connected: " + session);
        this.sessions.add(session);
        
      
    }
    
	@Override
	public void handleBinaryMessage(WebSocketSession session, BinaryMessage message){
        queue.add(to_bytes(message.getPayload()));
	}

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[CaptureStreamWS] Client disconnected: " + session);
        this.sessions.remove(session);
    }
}
