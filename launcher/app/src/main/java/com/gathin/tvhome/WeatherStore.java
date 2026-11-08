package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;

/** Private city selection and last valid forecast, scoped to this launcher. */
final class WeatherStore {
    private static final String NAME = "weather";
    private final SharedPreferences preferences;

    WeatherStore(Context context) {
        preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    WeatherClient.City city() {
        if (!preferences.contains("latitude") || !preferences.contains("longitude")) return null;
        return new WeatherClient.City(preferences.getString("city", ""),
                Double.longBitsToDouble(preferences.getLong("latitude", 0)),
                Double.longBitsToDouble(preferences.getLong("longitude", 0)));
    }

    void setCity(WeatherClient.City city) {
        preferences.edit().putString("city", city.name)
                .putLong("latitude", Double.doubleToRawLongBits(city.latitude))
                .putLong("longitude", Double.doubleToRawLongBits(city.longitude))
                .remove("snapshot").remove("fetched").apply();
    }

    void save(JSONObject snapshot, long fetched) {
        preferences.edit().putString("snapshot", snapshot.toString()).putLong("fetched", fetched).apply();
    }

    long fetched() { return preferences.getLong("fetched", 0); }

    WeatherClient.Snapshot snapshot(long now) {
        long age = now - fetched();
        if (age < 0 || age > 6 * 60 * 60_000L) return null;
        String saved = preferences.getString("snapshot", null);
        if (saved == null) return null;
        try {
            WeatherClient.Snapshot snapshot = WeatherClient.parse(new JSONObject(saved));
            return snapshot.reasonableAt(now) ? snapshot : null;
        } catch (JSONException | RuntimeException invalid) {
            return null;
        }
    }
}
