package com.pasystem.client;

import com.pasystem.client.NetworkMgr;
import com.pasystem.client.ConnectionLoop;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



@SpringBootApplication
public class App {

    private NetworkMgr __network_mgr__ = new NetworkMgr("wlan0"); //added wlan0 just bc my thing has 2 interfaces wlan0 and virbr0
	private ConnectionLoop __connection__loop = new ConnectionLoop();
    public static void main(String[] args) {
        
        SpringApplication.run(App.class, args);
        	// add connection loop logic
    // add WS connect socket logic

    }
}
