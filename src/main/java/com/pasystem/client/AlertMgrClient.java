package com.pasystem.client;

import com.pasystem.client.SpeakerMgr;
import java.io.FileInputStream;
import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.nio.file.Path;
//add playing sound logic
public class AlertMgrClient {
	
	private static String path = ""; //keep this in mind ig, we could mak ethis a hashmap that load from json and if not there then check in resources and if still nothing then print not found ig	

	private static class SoundAlert {
		//implement start/stop per alert
		
		private Clip clip;
		private String file_path = null;
		private boolean playing = false; 
		private FileInputStream file_stream = null;
		private String alert = null;
		public SoundAlert(String alert) {
			this.alert = alert;
			System.out.println("[SoundAlert] Searching for sound file for alert '" + alert + "'");
			
			List<String> possible_paths = List.of(alert + ".wav", Path.of(path, alert + ".wav").toString());
			for (int i = 0; i < possible_paths.size(); i++) {	
				System.out.println("[SoundAlert] Checking for sound file for alert '" + alert + "' in filepath '" + possible_paths.get(i) + "'");
				if(new File(possible_paths.get(i)).exists()) {
					file_path = possible_paths.get(i);
					System.out.println("[SoundAlert] Found file in path '" + possible_paths.get(i) + "'" + "");
				}
			}			
			
			if (file_path == null) {
				System.out.println("[SoundAlert] Could not find alert sound for '" + alert + "'");
				clip = null;
				start();
				return;	
			}
	
			System.out.println("[SoundAlert] Creating Clip object via SpeakerMgr and automatically playing it");
			try {
				file_stream = new FileInputStream(new File(file_path));
			} catch (Exception e) {	
				System.out.println("\n[SoundAlert] It seems as the file was not found even though we JUST checked for it");
				System.out.println("\tThis should (theoritcally) never run, and if you are seeing this,");
				System.out.println("\tthere was a massive oversight made when writing this class\n");			
			}

			clip = SpeakerMgr.play_file(file_stream);
			
			start();
		}
		

		
	public void start() {
		try {
			if (clip == null) {	
				System.out.println("[SoundAlert] It seems as the speaker was down when creating '" + alert + "'");

				Clip test = null;

				try { 		
					test = SpeakerMgr.play_file(
							new FileInputStream(new File(file_path))
					);
				} catch (Exception e) {	
					System.out.println("\n[SoundAlert] It seems as the file was not found even though we already checked for it");
					System.out.println("\tThis should (theoritcally) never run, and if you are seeing this,");
					System.out.println("\tthere was a massive oversight made when writing this class\n");			
					e.printStackTrace();
				}

				if (test == null) {
					System.out.println("[SoundAlert] Playing emergency buzzer '" + alert + "'");
					playing = true;
					return;
				} else {
					System.out.println("[SoundAlert] Speaker is back up");
					clip = test;	
				}

			} else if (!clip.isOpen()) {
				System.out.println("[SoundAlert] Clip was taken down by SpeakerMgr for reconnect reasons");

				if (SpeakerMgr.is_up()) {

					try {
						clip = SpeakerMgr.play_file(
								new FileInputStream(new File(file_path))
						);
					} catch (Exception e) {	
						System.out.println("\n[SoundAlert] It seems as the file was not found even though we aleady checked for it");
						System.out.println("\tThis should (theoritcally) never run, and if you are seeing this,");
						System.out.println("\tthere was a massive oversight made when writing this class\n");			
						e.printStackTrace();
					}

				} else {
					System.out.println("[SoundAlert] Playing emergency buzzer '" + alert + "'");
					playing = true;
					return;
				}
			}

			if (clip != null) {
				clip.setFramePosition(0);
				clip.loop(Clip.LOOP_CONTINUOUSLY);
				playing = true;
			}

		} catch (Exception e) {
			System.out.println("[SoundAlert] Unexpected error while starting '" + alert + "'");
			e.printStackTrace();
		}
	}
	public void stop() {
		if (!playing) {
			return;
		}

		if (clip == null) {
			System.out.println("[SoundAlert] Stopping emergency buzzer '" + alert + "'");
			// stop emergency buzzer
			playing = false;
			return;
		}

		if (!clip.isOpen()) {
			System.out.println("[SoundAlert] Clip was already closed by SpeakerMgr");
			playing = false;
			return;
		}

		clip.stop();
		playing = false;
	}
			
		public boolean playing() {
			return playing;			
		}
	}



	private static Map<String, SoundAlert> alerts = new HashMap<>();


	public static void set_alert(String alert) {
		if (alerts.containsKey(alert))	{
			if (alerts.get(alert).playing()) {
				System.out.println("[AlertMgrClient] Cannot play alert '" + alert + "' as it is already playing");
				return;
			}
			
			System.out.println("[AlertMgrClient] Playing alert '" + alert + "'");
			alerts.get(alert).start();
			return;

		}
		
		
		System.out.println("[AlertMgrClient] Adding + playing new alert '" + alert + "'");
		alerts.put(alert, new SoundAlert(alert));
		
	}


	public static void stop_alert(String alert) {
		if (!alerts.containsKey(alert)) {	
			System.out.println("[AlertMgrClient] The alert '" + alert + "' cannot be stopped because it doesn't exist");
			return;
		}
		
		if (!alerts.get(alert).playing()) {
			System.out.println("[AlertMgrClient] The alert '" + alert + "' cannot be stopped because it isnt playing");
			return;		
		}
		
		System.out.println("[AlertMgrClient] Stopping alert '" + alert + "'");
		alerts.get(alert).stop();
	
	}
	
	public static List<String> get_alerts() {
		return List.of(alerts.keySet().toArray(new String[0]));		
	}

	
}
