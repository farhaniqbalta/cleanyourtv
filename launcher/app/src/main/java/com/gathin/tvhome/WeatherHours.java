package com.gathin.tvhome;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Selects future forecast instants independently of the device's display timezone. */
final class WeatherHours {
    static final class Hour {
        final Instant time;
        final double temperature;
        final int code;

        Hour(Instant time, double temperature, int code) {
            this.time = time;
            this.temperature = temperature;
            this.code = code;
        }
    }

    static List<Hour> next(List<Hour> hours, Instant now) {
        List<Hour> future = new ArrayList<>();
        for (Hour hour : hours) if (hour != null && hour.time.isAfter(now)) future.add(hour);
        future.sort(Comparator.comparing(hour -> hour.time));
        return new ArrayList<>(future.subList(0, Math.min(4, future.size())));
    }

    private WeatherHours() { }
}
