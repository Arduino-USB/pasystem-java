package com.pasystem.client;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
public class GPIOMgr {
	
		
	class GPIOPin {
		public DigitalInput pin_obj;
		public String name;
		public boolean active;
		
		public GPIOPin(DigitalInput pin_obj, String name) {
			this.pin_obj = pin_obj;
			this.name = name;	
		}
	}
	
	
	private static ArrayList<GPIOPin> gpio_list = new ArrayList<>();
	private static boolean instance_created = false; 
	public GPIOMgr(Map<String,Integer> gpio_map) {
		
		if (instance_created) {
			System.out.println("[GPIOMgr] Only one instance is allowed to exist, then this is meant to be used as a static utility");
			return;	
		} else {
			instance_created = true;	
		}
		
		String[] gpio_map_names = gpio_map.keySet().toArray(new String[0]);
	
		Context pi4j = null;	

		try {
			pi4j = Pi4J.newAutoContext();
		} catch (Exception e) {
			System.out.println("[GPIOMgr] Either gpiod is not installed or this device is not a raspberry pi. Ignoring gpio pins");	
			return;
		}
		System.out.println("[GPIOMgr] Creating GPIO objects");
		for (int i = 0; i < gpio_map_names.length; i++) {
			int current_pin = gpio_map.get(gpio_map_names[i]);
			
				System.out.println("[GPIOMgr] Assiging label '" + gpio_map_names[i] + "' to pin '" + current_pin + "'");
				gpio_list.add(new GPIOPin(
				    pi4j.create(
				        DigitalInput.newConfigBuilder(pi4j)
				        .id(gpio_map_names[i])
				        .address(current_pin)
				        .pull(PullResistance.PULL_UP)
				        .build() 
				    ),
					gpio_map_names[i] 
				));	
			
		}
	
		new Thread(() -> {
			while (true) { 
				try {Thread.sleep(100);} catch (Exception e) {
					System.out.println("[GPIOMgr] There was an error sleeping, " + e);
				}
				
				for (int i = 0; i < gpio_list.size(); i++) {
					GPIOPin current_pin = gpio_list.get(i);
					// 1 = HIGH, 0 = LOW
					if (current_pin.pin_obj.state().equals(0)) {
						System.out.println("[GPIOMgr] Pin with label '" + current_pin.name + "' has been triggerd");
						current_pin.active = true;
					}
				} 	
			}
		}).start();
		
	}	
	
	public static List<String> get_pressed() {
		List<String> all_pressed = new ArrayList<>();
		
		for (int i = 0; i < gpio_list.size(); i++) {
			GPIOPin current_pin = gpio_list.get(i);
			if (current_pin.active) {
				all_pressed.add(current_pin.name);
				current_pin.active = false;	
			}	
		
		}
		
		return all_pressed;
				
	}

}
