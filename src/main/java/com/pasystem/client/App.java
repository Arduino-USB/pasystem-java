package com.pasystem.client;

import com.pasystem.client.NetworkMgr;
import com.pasystem.client.ConnectionLoop;
import com.pasystem.client.WSConnectionMgr;
import com.pasystem.client.SpeakerMgr;
import com.pasystem.client.MicMgr;
import com.pasystem.client.BellTrigger;

import com.pasystem.client.AudioRecvWS;
import com.pasystem.client.TimeSyncWS;

import com.pasystem.client.GPIOMgr;

import com.pasystem.client.PTTMgr;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.http.WebSocket;
import java.util.Map;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
		SpringApplication.run(App.class, args);
		new SpeakerMgr("Bar");
		new MicMgr("Bar");
		new NetworkMgr("");
		new ConnectionLoop(6741); 
		new WSConnectionMgr(
		    Map.<String, WebSocket.Listener>of(
		        "/client_stream", (WebSocket.Listener) new AudioRecvWS(),
		        "/time_stream", (WebSocket.Listener) new TimeSyncWS(),
		        "/audio_io", (WebSocket.Listener) new PTTMgr()
		    )
		);
		new BellTrigger();	
		new GPIOMgr(Map.of("ALARM", 18, "PTT", 17)); 
        

    }
}
