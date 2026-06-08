package com.pasystem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.pasystem.ClientMgr;
/**
 * Hello world!
 *
 */
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        ClientMgr client_mgr = new ClientMgr();
        SpringApplication.run(App.class, args);
    }
}