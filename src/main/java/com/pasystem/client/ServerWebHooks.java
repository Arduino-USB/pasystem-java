package com.pasystem.client;

import com.pasystem.client.SettingsMgr;
import com.pasystem.client.AlertMgrClient;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.List;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
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
		SettingsMgr.set_all_settings(settings);
		return Map.of("status", "ok");	
	}

	@GetMapping("/get_alerts")
	public List<String> get_alerts() {
		return AlertMgrClient.get_alerts();
	}
	
	// IN : {"alert" : "string"}	
	@PostMapping("/set_alert")
	public Map<String,String> set_alert(@RequestBody Map<String,String> data) {
		AlertMgrClient.set_alert(data.get("alert"));	
		return Map.of("status", "ok");
	}	
	
	@PostMapping("/stop_alert")
	public  Map<String,String> stop_alert(@RequestBody Map<String,String> data) {
		AlertMgrClient.stop_alert(data.get("alert"));
		return Map.of("status", "ok");
	}
	
    // just need a (/get_settings and /update_settings) and (/get_alerts, /set_alert, /disable_alert)
}
