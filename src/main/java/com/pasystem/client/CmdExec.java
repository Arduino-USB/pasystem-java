package com.pasystem.client;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class CmdExec {

	public static String run(String command) {
		try {
			ProcessBuilder builder = new ProcessBuilder(
				"/bin/sh",
				"-c",
				command
			);

			builder.redirectErrorStream(true);

			Process process = builder.start();

			String output;
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream())
			)) {
				output = reader.lines()
					.collect(Collectors.joining(System.lineSeparator()));
			}

			process.waitFor();
			return output;

		} catch (Exception e) {
			return "[CmdExec Error] Failed to execute command: " + e.getMessage();
		}
	}
}
