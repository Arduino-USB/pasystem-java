package com.pasystem.client;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;



@RestController
public class ServerWebHooks {
    
    @GetMapping("/alive")
    public Map<String, String> alive() {
        return Map.of("status", "O.K");
    }


    // just need a (/get_settings and /update_settings) and (/get_alerts, /set_alert, /disable_alert)
}