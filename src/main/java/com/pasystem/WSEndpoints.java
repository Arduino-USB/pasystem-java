package com.pasystem;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.pasystem.TimeStreamWS;


@Configuration
@EnableWebSocket
public class WSEndpoints implements WebSocketConfigurer {

    private final TimeStreamWS timeStream;


    public WSEndpoints(TimeStreamWS timeStream) {
        this.timeStream = timeStream;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(timeStream, "/time_stream").setAllowedOrigins("*");;
        //registry.addHandler(chatWS, "/chat");
    }
}