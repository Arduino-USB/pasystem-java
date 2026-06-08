package com.pasystem;

import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.time.Instant;


public class FakeClock {
    private static long offset = 0;
    private static String timezone = "Not/Set";

    public String get_timezone() {
        return timezone;
    }

    public void set_timezone(String timezone) {
        this.timezone = timezone;
    }
    public String get_time() {
        return Instant.parse(Instant.now().toString()).plusMillis(offset).toString();
    }

    public void set_time(String time_to_set) {
        long current_time = Instant.parse(Instant.now().toString()).toEpochMilli();
        long wanted_time = Instant.parse(time_to_set).toEpochMilli();

        offset = wanted_time - current_time;
    }
}