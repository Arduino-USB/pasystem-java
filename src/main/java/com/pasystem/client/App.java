package com.pasystem.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Imported the target components from com.crm
import com.pasystem.client.RenderMgr;
import com.pasystem.client.ClockDisplay;

import java.io.File;
import java.net.http.WebSocket;
import java.util.Collections;
import java.util.Map;

@SpringBootApplication
public class App {

    private static Map<String, Object> config = loadBootstrapConfig();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadBootstrapConfig() {
        File file = new File("bootstrap.json");
        if (!file.exists()) {
            System.out.println("[App] bootstrap.json not found. Running with empty configuration.");
            return Collections.emptyMap();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> loadedConfig = mapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            System.out.println("[App] Successfully loaded bootstrap.json");
            return loadedConfig != null ? loadedConfig : Collections.emptyMap();
        } catch (Exception e) {
            System.out.println("[App] Failed to parse bootstrap.json: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);

        // Core initializations
        new SpeakerMgr();
        new MicMgr();
        
        Object netPrefObj = config.get("networkInterface");
        String netPref = netPrefObj instanceof String ? (String) netPrefObj : "";
        new NetworkMgr(netPref);
        
        // Mandatory services
        new ConnectionLoop(6741); 
        new WSConnectionMgr(
            Map.<String, WebSocket.Listener>of(
                "/client_stream", (WebSocket.Listener) new AudioRecvWS(),
                "/time_stream", (WebSocket.Listener) new TimeSyncWS(),
                "/audio_io", (WebSocket.Listener) new PTTMgr()
            )
        );

        // BellTrigger: enabled ONLY if explicitly present and parses to true
        Object bellObj = config.get("bellEnabled");
        if (bellObj != null && Boolean.parseBoolean(bellObj.toString())) {
            new BellTrigger();
        }

        // Clock: enabled ONLY if clock is explicitly present and parses to true
        Object clockObj = config.get("clock");
        if (clockObj != null && Boolean.parseBoolean(clockObj.toString())) {
            new RenderMgr();
            new ClockDisplay();
        }

        // GPIOMgr: enabled ONLY if gpio_pins map is explicitly provided in the JSON
        Object gpioPinsObj = config.get("gpio_pins");
        if (gpioPinsObj instanceof Map) {
            try {
                Map<String, Integer> gpioMap = (Map<String, Integer>) gpioPinsObj;
                if (!gpioMap.isEmpty()) {
                    new GPIOMgr(gpioMap);
                }
            } catch (Exception e) {
                System.out.println("[App] Failed to parse gpio_pins: " + e.getMessage());
            }
        }

        // AlarmSender: enabled ONLY if alarmEnabled is true and GPIOMgr is active (requires "ALARM" pin mapping)
        Object alarmObj = config.get("alarmEnabled");
        if (alarmObj != null && Boolean.parseBoolean(alarmObj.toString())) {
            if (gpioPinsObj instanceof Map && ((Map<?, ?>) gpioPinsObj).containsKey("ALARM")) {
                new AlarmSender();
            } else {
                System.out.println("[App] AlarmSender enabled, but 'ALARM' pin is missing from 'gpio_pins' in bootstrap.json.");
            }
        }

        // SoundWatchdog: enabled ONLY if watchdogEnabled is true AND all parameters exist
        Object watchdogObj = config.get("watchdogEnabled");
        if (watchdogObj != null && Boolean.parseBoolean(watchdogObj.toString())) {
            Object spkObj = config.get("watchdogSpeaker");
            Object micObj = config.get("watchdogMic");
            Object cmdObj = config.get("watchdogCommand");

            if (spkObj instanceof String && micObj instanceof String && cmdObj instanceof String) {
                new SoundWatchdog((String) spkObj, (String) micObj, (String) cmdObj);
            } else {
                System.out.println("[App] SoundWatchdog enabled but missing required parameters (watchdogSpeaker, watchdogMic, or watchdogCommand) in bootstrap.json.");
            }
        }
    }
}
