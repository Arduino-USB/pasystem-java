package com.pasystem;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import java.util.ArrayList;


public class TimeStreamWS extends AbstractWebSocketHandler {
    private ArrayList<WebSocketSession> sessions = new ArrayList();

    @Override
    protected void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[TimeStreamWS] New client connected: " + session);
        this.sessions.add(session);

        try{
            new Thread(() -> {

            });
        } catch (Exception e) {
            System.out.println("[TimeStreamWS] Error occoured ");
            System.out.println(e);
        }
    }

    @Override
    protected void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("[TimeStreamWS] Client disconnected: " + session);
        this.sessions.remove(session);
    }
}