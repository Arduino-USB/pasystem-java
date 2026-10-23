package com.pasystem.client;

import com.pasystem.client.SettingsMgr;
import com.pasystem.client.AlertMgrClient;
import com.pasystem.client.CmdExec;

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

	@PostMapping("/mic_override")
	public  Map<String,String> micOverride() {
		PTTMgr.micOn();
		return Map.of("status", "ok");
	}
	
	@PostMapping("/mic_normal")
	public  Map<String,String> micNormal() {
		PTTMgr.micOff();
		return Map.of("status", "ok");
	}

	@GetMapping("/mic_status")
	public  Map<String,String> micStatus() {
		boolean micOverriden = PTTMgr.micStatus();
		String status;

		if (micOverriden == true) {
			status = "override";		
		} else {
			status = "normal";
		}
		
		return Map.of("status", status);
	}
	
  	@PostMapping("/reboot")
	public void reboot() {
		CmdExec.run("reboot");
	}
}
