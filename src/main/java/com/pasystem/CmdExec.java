package com.pasystem;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class CmdExec {

    public static String run(String command) {
        try {
            // 1. Split the string by spaces to handle command + arguments correctly
            String[] cmdArray = command.split(" ");
            ProcessBuilder builder = new ProcessBuilder(cmdArray);
            
            // Redirect error stream to standard output stream so we catch failures too
            builder.redirectErrorStream(true); 
            
            // 2. Start the OS process
            Process process = builder.start();

            // 3. Read the stream lines and join them with newlines using Java Streams
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }

            // 4. Clean up and wait for the process to finish
            process.waitFor();
            return output;

        } catch (Exception e) {
            return "[CmdExec Error] Failed to execute command: " + e.getMessage();
        }
    }
}
