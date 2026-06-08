package com.pasystem;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.time.ZoneId;

import com.pasystem.ClientMgr;
import com.pasystem.TemplateRenderer;
import com.pasystem.CmdExec;
import com.pasystem.FakeClock;

import java.util.ArrayList;
import java.util.Map;


@RestController
public class ServerWebHooks {
    ClientMgr client_mgr = new ClientMgr();
    CmdExec cmd = new CmdExec();
    FakeClock clock = new FakeClock();
    TemplateRenderer renderer = new TemplateRenderer("templates");

    @GetMapping("/")
    public String index() {
        return renderer.render("main", Map.of());
    }

    @GetMapping("/register")
    public void register(HttpServletRequest request) {
        client_mgr.add_client(request.getRemoteAddr());
    }

    @GetMapping("/alive")
    public String register() {
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

    @PostMapping("/set_time")
    public void set_time(@RequestBody Map<String, Object> data) {
        clock.set_timezone((String) data.get("timezone"));
        
    }

}