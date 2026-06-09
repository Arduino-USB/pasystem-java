package com.pasystem;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.pasystem.TimeStreamWS;
import com.pasystem.SoundStreamWS;


@Configuration
@EnableWebSocket
public class WSEndpoints implements WebSocketConfigurer {

    private final TimeStreamWS time_stream;
	private final SoundStreamWS sound_stream;
	private final SpeakWS speak_ws;


    public WSEndpoints(TimeStreamWS time_stream, SoundStreamWS sound_stream, SpeakWS speak_ws) {
        this.time_stream = time_stream;
		this.sound_stream = sound_stream;
		this.speak_ws = speak_ws;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(time_stream, "/time_stream").setAllowedOrigins("*");
        registry.addHandler(sound_stream, "/client_stream").setAllowedOrigins("*");
		registry.addHandler(speak_ws, "/speak").setAllowedOrigins("*");
    }
}
