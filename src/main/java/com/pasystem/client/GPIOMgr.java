package com.pasystem.client;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class GPIOMgr {

	class GPIOPin {
		public String name;
		public boolean active;
		public boolean was_pressed;

		public GPIOPin(String name) {
			this.name = name;
			this.active = false;
			this.was_pressed = false;
		}
	}

	private static ArrayList<GPIOPin> gpio_list = new ArrayList<>();
	private static boolean instance_created = false;

	public GPIOMgr(Map<String, Integer> gpio_map) {

		if (instance_created) {
			System.out.println(
				"[GPIOMgr] Only one instance is allowed to exist, " +
				"then this is meant to be used as a static utility"
			);
			return;
		}

		instance_created = true;

		System.out.println("[GPIOMgr] Starting DESKTOP GPIO test mode");
		System.out.println(
			"[GPIOMgr] Type the first letter of a GPIO name to toggle it"
		);

		// Create fake GPIO pins
		for (String name : gpio_map.keySet()) {

			System.out.println(
				"[GPIOMgr] Creating simulated GPIO '" +
				name +
				"' (pin " +
				gpio_map.get(name) +
				")"
			);

			gpio_list.add(new GPIOPin(name));
		}

		// Input thread
		new Thread(() -> {

			Scanner scanner = new Scanner(System.in);

			while (true) {

				String input;

				try {
					input = scanner.nextLine().trim();
				} catch (Exception e) {

					System.out.println(
						"[GPIOMgr] Error reading input: " + e
					);

					break;
				}

				if (input.length() == 0) {
					continue;
				}

				char key = Character.toLowerCase(input.charAt(0));

				boolean found = false;

				for (GPIOPin current_pin : gpio_list) {

					if (Character.toLowerCase(
							current_pin.name.charAt(0)
						) == key) {

						// Toggle the GPIO state
						current_pin.active = !current_pin.active;

						// Keep was_pressed synchronized
						current_pin.was_pressed = current_pin.active;

						System.out.println(
							"[GPIOMgr] Simulated '" +
							current_pin.name +
							"' -> " +
							(current_pin.active ? "DOWN" : "UP")
						);

						found = true;
						break;
					}
				}

				if (!found) {

					System.out.println(
						"[GPIOMgr] No GPIO starts with '" +
						key +
						"'"
					);
				}
			}

		}, "GPIOMgr-Input").start();
	}

	/**
	 * Returns whether the GPIO is currently active.
	 *
	 * Unlike the previous implementation, this does NOT
	 * reset the GPIO state.
	 */
	public static boolean is_pressed(String name) {

		for (int i = 0; i < gpio_list.size(); i++) {

			GPIOPin current_pin = gpio_list.get(i);

			if (current_pin.name.equals(name)) {
				return current_pin.active;
			}
		}

		return false;
	}

	/**
	 * Returns whether the GPIO is currently being held down.
	 *
	 * In desktop test mode this is the same as is_pressed().
	 */
	public static boolean is_down(String name) {

		for (int i = 0; i < gpio_list.size(); i++) {

			GPIOPin current_pin = gpio_list.get(i);

			if (current_pin.name.equals(name)) {
				return current_pin.active;
			}
		}

		return false;
	}
}
