package com.pasystem;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.servlet.http.HttpServletRequest;

import com.pasystem.BellDataMgr.BellSchedule;
import com.pasystem.ClientMgr;
import com.pasystem.TemplateRenderer;
import com.pasystem.CmdExec;
import com.pasystem.FakeClock;
import com.pasystem.BellDataMgr;

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
	
    @GetMapping("/")
    public String index() {
        return renderer.render("main", Map.of());
    }

    @GetMapping("/config")
    public String config() {
        renderer.render("config", Map.of());
    }

    @GetMapping("/register")
    public String register(HttpServletRequest request) {
        client_mgr.add_client(request.getRemoteAddr());
		return "O.K";
    }
    
    @GetMapping("/alive")
    public String alive() {
        return "O.K";
    }

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
    public String set_time(@RequestBody HashMap<String, Object> data) {
        clock.set_timezone((String) data.get("timezone"));
        clock.set_time((String) data.get("time"));
		return "O.K";
    }
	
	// OUT {"name" : name, "times" [times], "active", t/f}
	@GetMapping("/get_bells")
	public ArrayList<BellSchedule> get_bells() {
		return bell_mgr.get_bells();
	}
	
	// IN: {"name" : name, "times" : times, "active" : false}
	@PostMapping("/new_bell")
	public String new_bell(@RequestBody BellSchedule bell_schedule) {
		bell_mgr.add(bell_schedule);
		return "O.K";
	}

	@PostMapping("/remove_bell")
	public String new_bell(@RequestBody String bell_name) {
		bell_mgr.remove(bell_name);
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


	
}
