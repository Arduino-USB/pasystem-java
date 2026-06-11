package com.pasystem;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.servlet.http.HttpServletRequest;

import com.pasystem.AlertMgr.ClientData;
import com.pasystem.AlertMgr;

import com.pasystem.BellDataMgr.BellSchedule;
import com.pasystem.BellDataMgr;

import com.pasystem.ClientMgr;
import com.pasystem.TemplateRenderer;
import com.pasystem.CmdExec;
import com.pasystem.FakeClock;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@RestController
public class ServerWebHooks {
    ClientMgr client_mgr = new ClientMgr();
    CmdExec cmd = new CmdExec();
    FakeClock clock = new FakeClock();
    TemplateRenderer renderer = new TemplateRenderer("templates");
	BellDataMgr bell_mgr = new BellDataMgr();
	AlertMgr alert_mgr = new AlertMgr();

    /* ui section */
    @GetMapping("/")
    public String index() {
        return renderer.render("main", Map.of());
    }

    @GetMapping("/config")
    public String config() {
        return renderer.render("config", Map.of());
    }

     @GetMapping("/bell_config")
    public String bell() {
        return renderer.render("bell", Map.of());
    }



    /* connect section */
    @GetMapping("/register")
    public String register(HttpServletRequest request) {
        client_mgr.add_client(request.getRemoteAddr());
		return "O.K";
    }
    
    @GetMapping("/alive")
    public String alive() {
        return "O.K";
    }


    /* user section */


    // OUT : [{"ip" : "string", "name" : "string"}]
    @GetMapping("/get_users")
    public ArrayList<Map<String,String>> get_users() {
        return client_mgr.get_clients();
    }

    /* time section */

    @GetMapping("/get_time")
    public String get_time() {
        return clock.get_time();
    }
    
    @GetMapping("/get_timezone")
    public String get_timezone() {
        return clock.get_timezone();
    }

    @GetMapping("/get_timezones")
    public ArrayList get_timezones() {
        return new ArrayList<>(ZoneId.getAvailableZoneIds());
    }

	// IN: {"time" : time, "timezone" : timezone}
    @PostMapping("/set_time")
    public String set_time(@RequestBody HashMap<String, String> data) {
        clock.set_timezone(data.get("timezone"));
        clock.set_time(data.get("time"));
		return "O.K";
    }
	



    /* bell scetion */
	// OUT [{"name" : name, "times" [times], "active", t/f}]
	@GetMapping("/get_bells")
	public ArrayList<BellSchedule> get_bells() {
		return bell_mgr.get_bells();
	}

    @GetMapping("/get_active_bell_times")
    public ArrayList<String> get_active_bell_times() {
        return bell_mgr.get_active_times();
    }
	
	// IN: {"name" : name, "times" : [times], "active" :  t/f}
	@PostMapping("/new_bell")
	public String new_bell(@RequestBody BellSchedule bell_schedule) {
		bell_mgr.add(bell_schedule);
		return "O.K";
	}

    // {"name" : "string", "times", ["time", "time"]}
    @PostMapping("/edit_bell")
    public String edit_bell(@RequestBody Map<String, Object> data) {
        bell_mgr.edit((String) data.get("name"), (ArrayList<String>) data.get("times"));
        return "O.K";
    }

	@PostMapping("/remove_bell")
	public String new_bell(@RequestBody String bell_name) {
		bell_mgr.remove_bell(bell_name);
		return "O.K";
	}

	@PostMapping("/activate_bell")
	public String activate_bell(@RequestBody String bell_name) {
		bell_mgr.activate(bell_name);
		return "O.K";
	}
	
	@PostMapping("/disable_bell")
	public String disable_bell(@RequestBody String bell_name) {
		bell_mgr.disable(bell_name);
		return "O.K";
	}


    /* alert section */
    // OUT : [{"ip" : "string", "active" : "string/null"}]
    @GetMapping("/get_all_alerts")
    public ArrayList<ClientData> get_active_alerts() {
        return alert_mgr.get_all_alerts();
    }

    @PostMapping("/set_alert")
    public String set_alert_status(@RequestBody Map<String, String> client) {
        alert_mgr.set_alert(client.get("ip"), client.get("alert"));
        return "O.K";
    }

    @PostMapping("/remove_alert")
    public String remove_alert(@RequestBody Map<String, String> client) {
        alert_mgr.set_alert(client.get("ip"), client.get("alert"));
        return "O.K";
    }


	
}
