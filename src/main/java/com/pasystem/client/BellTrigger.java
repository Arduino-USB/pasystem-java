package com.pasystem.client;

import com.fasterxml.jackson.databind.util.ExceptionUtil;
import com.pasystem.client.ConnectionLoop;
import com.pasystem.client.SpeakerMgr;
import com.pasystem.client.HTTPWrapper;
import com.pasystem.client.FakeClock;

import java.io.FileInputStream;
import javax.sound.sampled.*;
import java.util.ArrayList;
import java.time.Instant;
import java.io.File;

import java.time.LocalTime;
import java.time.Duration;

public class BellTrigger {	
	private boolean instance_created = false;
	private ArrayList<String> bells = new ArrayList<>();
	private HTTPWrapper requests = new HTTPWrapper();
	private FakeClock clock = new FakeClock();
	private long last_trigger = 0;


	public BellTrigger() {
		if (instance_created) {	
			System.out.println("[BellTrigger] This class is only allowed to have one instance, then it is meant to be used as a static utility");
			return;	
		}
	

			
		new Thread(() -> {	
			System.out.println("[BellTrigger] Starting bell times thread");
			while (true) {
				
				try {Thread.sleep(300);} catch (Exception e) {
					System.out.println("[BellTrigger] There was a problem sleeping, " + e);
				}
				
				
				if (ConnectionLoop.connected()) {
					try {	
						bells = requests.get("http://" + ConnectionLoop.get_ip() + ":" + ConnectionLoop.get_port() + "/get_active_bell_times", ArrayList.class);
					} catch (Exception e) {
						System.out.println("[BellTrigger] There seems to be a problem updating the bells, " +  e);
					}					
				
				
					for (int i = 0; i < bells.size(); i++) {
						/*
						System.out.println(
							"[BellTrigger] Debug time info" + 
						    "\n\tCurrent: " + clock.get_time_clean() +
						    "\n\tBell: " + bells.get(i)
						);
						*/
				
						//i know this line is long but this is to check if the current set time is 2500 (2.5 seconds) ms or less away from the bell time
						if (Math.abs(Duration.between(LocalTime.parse(clock.get_time_clean()), LocalTime.parse(bells.get(i))).toMillis()) <= 500) {
							
							if (clock.get_real_timestamp() - last_trigger < 5000){
								System.out.println("[BellTrigger] It hasn't been long enough since the last bell played. Skipping");
								continue;
							} else {
								last_trigger = clock.get_real_timestamp(); 
							}
		
							if(SpeakerMgr.is_up()){
								if (new File("bell.wav").exists()) {
									System.out.println("[BellTrigger] Playing bell sound");	
									try {
										SpeakerMgr.play_file(new FileInputStream(new File("bell.wav")));
									} catch (Exception e) {
										System.out.println("[BellTrigger] There was a problem playing the bell sound, " + e);
									}	
								} else {
									System.out.println("[BellTrigger] Cannot find bell tone file path. Skipping");
								}
							} else {
								System.out.println("[BellTrigger] Cannot play bell sound as speaker isnt connected");
							}
						}
					}
				}
			}	
			
			
			
		}).start();
		
		instance_created = true;
		
	
			
	}		
}
