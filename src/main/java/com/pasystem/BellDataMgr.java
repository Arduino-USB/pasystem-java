// This class will implement 
package com.pasystem;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class BellDataMgr {


	private static class BellSchedule {
		public String name = "Default";
		public ArrayList<String> times = new ArrayList(List.of("12:00"));
	}
	
	private static ArrayList<BellSchedule> schedules;
	private static String filename = "schedules.json";	
		
	public static BellDataMgr() {
		System.out.println("[BellDataMgr] Starting");
		File config = new File(filename);
		
		if (config.exists()) {
			System.out.println("[BellDataMgr] Config exists, loading");
			schedules = new ObjectMapper().readValue(
				config,
				new TypeReference<ArrayList<BellSchedule>>() {}
			);	
		} else {
			System.out.println("[BellDataMgr] Config nonexistant, creating empty list");
			schedules = new TypeReference<ArrayList<BellSchedule>>() {}
		}
			
		
	}
	
	
	
	public static ArrayList<String> get_bells() {
		ArrayList<String> bells = new ArrayList();
		for(int i = 0; i < times.size(); i++) {	
			bells.add(times.get(i).name);
		}
		
		return bells;
	}
	
	public static boolean add(String name, ArrayList<String> times) {
		for(int i = 0; i < times.size(); i++) {		
			if (schedules.get(i).name.equals(name)) {
				System.out.println("[BellDataMgr] Can't add bell '" + name + "' as it already exists");
				return false;
			}
		}
		
		System.out.println("[BellDataMgr] Added bell '" + name + "'");
		schedule = new BellSchedule();
		schedule.name = name;
		schedule.times = times;
		schedules.add(schedule);
		
		return true;
	
	}

	public static void save() {	
		File config = new File(filename);
		
		System.out.println("[BellDataMgr] Writing to config file");
		
		new ObjectMapper().writeValue(
			new File(filename),
			schedules
		);
		
	}

}
