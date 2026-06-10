package com.pasystem;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class BellDataMgr {


	public static class BellSchedule {
		public String name = "Default";
		public ArrayList<String> times = new ArrayList<>(List.of("12:00"));
		public boolean active = false;
	}
	
	private static ArrayList<BellSchedule> schedules;
	private static String filename = "schedules.json";	
		
	public BellDataMgr() {
		System.out.println("[BellDataMgr] Starting");
		File config = new File(filename);
		

		try {
			if (config.exists()) {
				System.out.println("[BellDataMgr] Config exists, loading");
				schedules = new ObjectMapper().readValue(
					config,
					new TypeReference<ArrayList<BellSchedule>>() {}
				);	
			} else {
				System.out.println("[BellDataMgr] Config nonexistant, creating empty list");
				schedules = new ArrayList<>();
			}
		} catch (Exception e){
			System.out.println("[BellDataMgr] Error loading JSON, " + e);	
			System.out.println("[BellDataMgr] Ignoring JSON file, creating empty list");
			schedules = new ArrayList<>();	
		}		
		
	}
	
	
	public static void remove_bell(String name) {
		
		for (int i = 0; i < schedules.size(); i++){
			if (schedules.get(i).name.equals(name)) {
				System.out.println("[BellDataMgr] Removing bell '" + name + "'");
				schedules.remove(i);
			}
		}	
		
	}
	
	public static void activate(String name){
		for (int i = 0; i < schedules.size(); i++){
			if (schedules.get(i).name.equals(name)) {
				System.out.println("[BellDataMgr] Activiating bell '" + name + "'");
				schedules.get(i).active = true;
			}
		}		
	}
	public static void disable(String name){
		for (int i = 0; i < schedules.size(); i++){
			if (schedules.get(i).name.equals(name)) {
				System.out.println("[BellDataMgr] Deactiviating bell '" + name + "'");
				schedules.get(i).active = false;
			}
		}	

	}

	public static ArrayList<String> get_bell_names() {
		ArrayList<String> bells = new ArrayList();
		for(int i = 0; i < schedules.size(); i++) {	
			bells.add(schedules.get(i).name);
		}
		
		return bells;
	}
	

	public static ArrayList<BellSchedule> get_bells() {
		return schedules;
	}

	public static void add(BellSchedule bell_schedule) {
		for(int i = 0; i < schedules.size(); i++) {		
			if (schedules.get(i).name.equals(bell_schedule.name)) {
				System.out.println("[BellDataMgr] Warning: Bell '" + name + "' already exists, overwriting");
				schedules.remove(i);
			}
		}
		
		System.out.println("[BellDataMgr] Added bell '" + name + "'");
		schedules.add(bell_schedule);
		
	
	}

	public static void save() {	
		File config = new File(filename);
		
		System.out.println("[BellDataMgr] Writing to config file");
		
		try {
			new ObjectMapper().writeValue(
				new File(filename),
				schedules
			);
		} catch (Exception e) {
			System.out.println("[BellDataMgr] Error saving JSON, " + e);
			System.out.println("[BellDataMgr] Not saving JSON");	
		}
		
	}
	
	

}
