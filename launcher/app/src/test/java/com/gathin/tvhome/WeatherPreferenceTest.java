package com.gathin.tvhome;

import static org.junit.Assert.*;
import org.junit.Test;

public class WeatherPreferenceTest {
    @Test public void explicitChoiceOverridesEitherThemeValue() {
        assertFalse(WeatherPreference.resolve(true, false));
        assertTrue(WeatherPreference.resolve(false, true));
    }

    @Test public void noChoiceFollowsTheme() {
        assertTrue(WeatherPreference.resolve(true, null));
        assertFalse(WeatherPreference.resolve(false, null));
    }
}
