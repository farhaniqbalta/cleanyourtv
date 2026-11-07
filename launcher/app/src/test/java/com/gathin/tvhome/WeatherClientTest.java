package com.gathin.tvhome;

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.Instant;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class WeatherClientTest {
    private static JSONObject response() throws JSONException {
        return new JSONObject("{\"timezone\":\"Asia/Jakarta\","
                + "\"current\":{\"time\":1793912400,\"temperature_2m\":29.1,\"weather_code\":3},"
                + "\"hourly\":{\"time\":[1793912400,1793916000],"
                + "\"temperature_2m\":[30,31],\"weather_code\":[1,2]}}");
    }

    @Test public void parsesCurrentAndHourlyInstants() throws Exception {
        WeatherClient.Snapshot snapshot = WeatherClient.parse(response());
        assertEquals(29.1, snapshot.temperature, 0.01);
        assertEquals("Asia/Jakarta", snapshot.timezone.getId());
        assertEquals(2, snapshot.hours.size());
        assertEquals(Instant.ofEpochSecond(1793912400), snapshot.hours.get(0).time);
    }

    @Test public void rejectsMismatchedArraysAndMissingCurrent() throws Exception {
        JSONObject mismatch = response();
        mismatch.getJSONObject("hourly").getJSONArray("time").put(123);
        assertThrows(JSONException.class, () -> WeatherClient.parse(mismatch));
        JSONObject missing = response();
        missing.remove("current");
        assertThrows(JSONException.class, () -> WeatherClient.parse(missing));
    }

    @Test public void rejectsStaleOrFutureCurrentObservation() throws Exception {
        WeatherClient.Snapshot snapshot = WeatherClient.parse(response());
        long observed = 1793912400L * 1000;
        assertTrue(snapshot.reasonableAt(observed + 60_000));
        assertFalse(snapshot.reasonableAt(observed + 7 * 60 * 60_000L));
        assertFalse(snapshot.reasonableAt(observed - 6 * 60_000L));
    }

    @Test public void cancelledRequestDoesNotOpenAConnection() {
        WeatherClient.Request request = new WeatherClient.Request();
        request.cancel();
        assertThrows(IOException.class, () -> request.get("https://example.com"));
    }

    @Test public void timeoutIsPropagatedAndConnectionClosed() throws Exception {
        class TimedOut extends HttpURLConnection {
            boolean disconnected;
            TimedOut() throws Exception { super(new URL("https://example.com")); }
            @Override public void disconnect() { disconnected = true; }
            @Override public boolean usingProxy() { return false; }
            @Override public void connect() { }
            @Override public int getResponseCode() throws IOException { throw new SocketTimeoutException(); }
        }
        TimedOut connection = new TimedOut();
        assertThrows(SocketTimeoutException.class, () -> WeatherClient.read(connection));
        assertTrue(connection.disconnected);
        assertEquals(8000, connection.getConnectTimeout());
        assertEquals(8000, connection.getReadTimeout());
    }

    @Test public void nonSuccessResponseIsRejectedAndClosed() throws Exception {
        class Failure extends HttpURLConnection {
            boolean disconnected;
            Failure() throws Exception { super(new URL("https://example.com")); }
            @Override public void disconnect() { disconnected = true; }
            @Override public boolean usingProxy() { return false; }
            @Override public void connect() { }
            @Override public int getResponseCode() { return 503; }
        }
        Failure connection = new Failure();
        assertThrows(IOException.class, () -> WeatherClient.read(connection));
        assertTrue(connection.disconnected);
    }
}
