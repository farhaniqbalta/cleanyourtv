package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class WeatherHoursTest {
    private static WeatherHours.Hour hour(String time) {
        return new WeatherHours.Hour(Instant.parse(time), 25, 0);
    }

    @Test public void selectsFourUpcomingHoursAcrossMidnight() {
        List<WeatherHours.Hour> result = WeatherHours.next(Arrays.asList(
                hour("2026-11-05T23:00:00Z"), hour("2026-11-06T00:00:00Z"),
                hour("2026-11-06T01:00:00Z"), hour("2026-11-06T02:00:00Z"),
                hour("2026-11-06T03:00:00Z"), hour("2026-11-06T04:00:00Z")),
                Instant.parse("2026-11-05T23:50:00Z"));
        assertEquals(4, result.size());
        assertEquals(Instant.parse("2026-11-06T00:00:00Z"), result.get(0).time);
        assertEquals(Instant.parse("2026-11-06T03:00:00Z"), result.get(3).time);
    }

    @Test public void skipsElapsedAndHandlesMissingHours() {
        List<WeatherHours.Hour> result = WeatherHours.next(Arrays.asList(
                hour("2026-11-05T12:00:00Z"), hour("2026-11-05T10:00:00Z"),
                hour("2026-11-05T11:00:00Z")), Instant.parse("2026-11-05T10:50:00Z"));
        assertEquals(2, result.size());
        assertEquals(Instant.parse("2026-11-05T11:00:00Z"), result.get(0).time);
    }

    @Test public void selectsElevenThroughFourteenAfterTenFifty() {
        List<WeatherHours.Hour> result = WeatherHours.next(Arrays.asList(
                hour("2026-11-05T10:00:00Z"), hour("2026-11-05T11:00:00Z"),
                hour("2026-11-05T12:00:00Z"), hour("2026-11-05T13:00:00Z"),
                hour("2026-11-05T14:00:00Z"), hour("2026-11-05T15:00:00Z")),
                Instant.parse("2026-11-05T10:50:00Z"));
        assertEquals(4, result.size());
        assertEquals(Instant.parse("2026-11-05T11:00:00Z"), result.get(0).time);
        assertEquals(Instant.parse("2026-11-05T14:00:00Z"), result.get(3).time);
    }

    @Test public void choosesFutureInstantsAcrossDaylightSavingFallback() {
        ZoneId zone = ZoneId.of("America/New_York");
        Instant now = ZonedDateTime.parse("2026-11-01T01:50:00-04:00[America/New_York]").toInstant();
        List<WeatherHours.Hour> result = WeatherHours.next(Arrays.asList(
                hour("2026-11-01T05:00:00Z"), hour("2026-11-01T06:00:00Z"),
                hour("2026-11-01T07:00:00Z"), hour("2026-11-01T08:00:00Z"),
                hour("2026-11-01T09:00:00Z")), now);
        assertEquals(4, result.size());
        assertEquals(1, result.get(0).time.atZone(zone).getHour());
        assertEquals(Instant.parse("2026-11-01T06:00:00Z"), result.get(0).time);
    }

    @Test public void excludesTheCurrentExactHour() {
        List<WeatherHours.Hour> result = WeatherHours.next(Arrays.asList(
                hour("2026-11-05T11:00:00Z"), hour("2026-11-05T12:00:00Z")),
                Instant.parse("2026-11-05T11:00:00Z"));
        assertEquals(1, result.size());
        assertEquals(Instant.parse("2026-11-05T12:00:00Z"), result.get(0).time);
    }
}
