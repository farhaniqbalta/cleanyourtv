package com.gathin.tvhome;

/** An explicit TV choice takes priority over the active theme's default. */
final class WeatherPreference {
    static boolean resolve(boolean theme, Boolean choice) {
        return choice == null ? theme : choice;
    }

    private WeatherPreference() { }
}
