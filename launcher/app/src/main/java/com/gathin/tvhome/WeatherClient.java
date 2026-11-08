package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Open-Meteo access adapted from the standalone weather add-on. Call only on an I/O thread. */
final class WeatherClient {
    static final class City {
        final String name;
        final double latitude, longitude;
        City(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    static final class Snapshot {
        final double temperature;
        final int code;
        final ZoneId timezone;
        final long observedAt;
        final List<WeatherHours.Hour> hours;
        Snapshot(double temperature, int code, ZoneId timezone, long observedAt, List<WeatherHours.Hour> hours) {
            this.temperature = temperature;
            this.code = code;
            this.timezone = timezone;
            this.observedAt = observedAt;
            this.hours = hours;
        }

        boolean reasonableAt(long now) {
            return observedAt <= now + 5 * 60_000L && observedAt >= now - 6 * 60 * 60_000L;
        }
    }

    static List<City> search(String query, Request request) throws IOException, JSONException {
        JSONObject json = request.get("https://geocoding-api.open-meteo.com/v1/search?count=5&format=json&name="
                + URLEncoder.encode(query.trim(), "UTF-8") + "&language=" + Locale.getDefault().getLanguage());
        JSONArray results = json.optJSONArray("results");
        List<City> cities = new ArrayList<>();
        if (results == null) return cities;
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            String name = item.getString("name");
            String region = item.optString("admin1", "");
            String country = item.optString("country", "");
            if (!region.isEmpty() && !region.equals(name)) name += ", " + region;
            if (!country.isEmpty()) name += ", " + country;
            double latitude = item.getDouble("latitude"), longitude = item.getDouble("longitude");
            if (Double.isFinite(latitude) && latitude >= -90 && latitude <= 90
                    && Double.isFinite(longitude) && longitude >= -180 && longitude <= 180)
                cities.add(new City(name, latitude, longitude));
        }
        return cities;
    }

    static final class Request {
        private volatile HttpURLConnection connection;
        private volatile boolean cancelled;

        void cancel() {
            cancelled = true;
            HttpURLConnection active = connection;
            if (active != null) active.disconnect();
        }

        JSONObject get(String address) throws IOException, JSONException {
            if (cancelled) throw new IOException("Cancelled");
            HttpURLConnection active = (HttpURLConnection) new URL(address).openConnection();
            connection = active;
            if (cancelled) { active.disconnect(); throw new IOException("Cancelled"); }
            try { return read(active); }
            finally { connection = null; }
        }
    }

    static JSONObject fetch(double latitude, double longitude, Request request) throws IOException, JSONException {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f"
                + "&current=temperature_2m,weather_code&hourly=temperature_2m,weather_code"
                + "&timeformat=unixtime&timezone=auto&forecast_days=2", latitude, longitude);
        JSONObject json = request.get(url);
        parse(json); // Reject incomplete responses before caching them.
        return json;
    }

    static Snapshot parse(JSONObject json) throws JSONException {
        JSONObject current = json.getJSONObject("current");
        JSONObject hourly = json.getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        JSONArray temperatures = hourly.getJSONArray("temperature_2m");
        JSONArray codes = hourly.getJSONArray("weather_code");
        if (times.length() != temperatures.length() || times.length() != codes.length())
            throw new JSONException("Mismatched hourly arrays");
        List<WeatherHours.Hour> hours = new ArrayList<>();
        for (int i = 0; i < times.length(); i++) {
            if (times.isNull(i) || temperatures.isNull(i) || codes.isNull(i)) continue;
            double temperature = temperatures.getDouble(i);
            if (!Double.isFinite(temperature)) continue;
            hours.add(new WeatherHours.Hour(Instant.ofEpochSecond(times.getLong(i)), temperature, codes.getInt(i)));
        }
        ZoneId zone;
        try { zone = ZoneId.of(json.getString("timezone")); }
        catch (RuntimeException invalid) { throw new JSONException("Invalid timezone"); }
        double currentTemperature = current.getDouble("temperature_2m");
        if (!Double.isFinite(currentTemperature)) throw new JSONException("Invalid temperature");
        return new Snapshot(currentTemperature, current.getInt("weather_code"), zone,
                Math.multiplyExact(current.getLong("time"), 1000L), hours);
    }

    static JSONObject read(HttpURLConnection connection) throws IOException, JSONException {
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Weather request failed");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (out.size() > 512 * 1024) throw new IOException("Weather response too large");
                }
                return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    private WeatherClient() { }
}
