package com.pasystem.client;

import com.pasystem.client.SettingsMgr;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;



@RestController
public class ServerWebHooks {
    
		
    @GetMapping("/alive")
    public Map<String, String> alive() {
        return Map.of("status", "ok");
    }

	@GetMapping("/get_settings")
	public Map<String, String> get_settings() {
		return SettingsMgr.get_settings();
	}

	@PostMapping("/update_settings")
	public Map<String, String> update_settings(@RequestBody Map<String, String> settings) {
		SettingsMgr.set_settings(settings);
		return Map.of("yara yara", "phonk");	
	}	
	
    // just need a (/get_settings and /update_settings) and (/get_alerts, /set_alert, /disable_alert)
}
