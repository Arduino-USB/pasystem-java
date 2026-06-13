package com.pasystem.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SettingsMgr {

    private static Map<String,String> settings = new HashMap<>();

    public static Map<String,String> get_settings() {
        return settings;
    }

    public static String get_setting(String name) {
        String[] setting_keys = settings.keySet().toArray(new String[0]);
        for (int i = 0; i < setting_keys.length; i++){
            String current_setting = setting_keys[i];
            if (settings.get(current_setting).equals(name)){
                return settings.get(current_setting);
            }
        }
        return null;
    }


    public static void set_setting(String name, String value) {
        settings.put(name, value);
    }

    public static void set_all_settings(Map<String,String> new_settings) {
        settings = new_settings;
    }


}