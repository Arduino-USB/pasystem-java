package com.pasystem.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SettingsMgr {

    private static final String FILE_NAME = "settings.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<String, String> settings = loadAndApplySettings();

    private static Map<String, String> loadAndApplySettings() {
        File file = new File(FILE_NAME);
        Map<String, String> loadedSettings = null;

        if (file.exists()) {
            try {
                loadedSettings = objectMapper.readValue(file, new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                System.err.println("[SettingsMgr] Failed to load settings.json, falling back to defaults: " + e.getMessage());
            }
        }

        if (loadedSettings == null) {
            // File does not exist or failed to read, generate defaults with random name and save
            loadedSettings = new HashMap<>();
            loadedSettings.put("name", "Unassigned-" + new Random().nextInt(10000, 100000));
            loadedSettings.put("mic_vol", "100");
            loadedSettings.put("spk_vol", "100");
            
            saveToFile(loadedSettings);
        }

        // Apply volumes immediately upon loading/creation
        applyVolumes(loadedSettings);
        return loadedSettings;
    }

    private static synchronized void saveAndApply() {
        saveToFile(settings);
        applyVolumes(settings);
    }

    private static void saveToFile(Map<String, String> map) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_NAME), map);
        } catch (IOException e) {
            System.err.println("[SettingsMgr] Failed to save settings.json: " + e.getMessage());
        }
    }

    private static void applyVolumes(Map<String, String> targetSettings) {
        try {
            String spkVol = targetSettings.get("spk_vol");
            String micVol = targetSettings.get("mic_vol");
            
            if (spkVol != null) {
                CmdExec.run("pactl set-sink-volume @DEFAULT_SINK@ " + spkVol + "%");
            }
            if (micVol != null) {
                CmdExec.run("pactl set-source-volume @DEFAULT_SOURCE@ " + micVol + "%");
            }
        } catch (Exception e) {
            System.err.println("[SettingsMgr] Failed to execute volume commands: " + e.getMessage());
        }
    }

    // Returns a safe copy to prevent external code from mutating settings without saving
    public static synchronized Map<String, String> get_settings() {
        return new HashMap<>(settings);
    }

    public static synchronized String get_setting(String name) {
        return settings.get(name);
    }

    public static synchronized void set_setting(String name, String value) {
        settings.put(name, value);
        saveAndApply();
    }

    public static synchronized void set_all_settings(Map<String, String> new_settings) {
        System.out.println("[SettingsMgr] Set all settings to '" + new_settings + "'");
        settings = new_settings != null ? new HashMap<>(new_settings) : new HashMap<>();
        saveAndApply();
    }
}
