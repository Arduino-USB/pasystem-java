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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
		new SpeakerMgr("alsa");
		new MicMgr("alsa");
		new NetworkMgr("wlan0");
		new ConnectionLoop(6741); 
		new WSConnectionMgr(Map.of("/client_stream", new AudioRecvWS(), "/time_stream", new TimeSyncWS()));
		new BellTrigger();	
		new GPIOMgr(Map.of("ALARM", 17)); 
        SpringApplication.run(App.class, args);

    }
}
