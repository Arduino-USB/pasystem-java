package com.pasystem.client;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.pasystem.client.NetworkMgr;

@SpringBootApplication
public class App {

    NetworkMgr __network_mgr__ = new NetworkMgr();
    public static void main(String[] args) {
        
        SpringApplication.run(App.class, args);
        	// add connection loop logic
    // add WS connect socket logic

    }
}