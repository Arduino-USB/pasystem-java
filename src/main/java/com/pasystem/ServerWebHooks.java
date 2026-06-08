package com.pasystem;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import com.pasystem.ClientMgr;

import com.pasystem.TemplateRenderer;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;


@RestController
public class ServerWebHooks {
    ClientMgr client_mgr = new ClientMgr();
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
}