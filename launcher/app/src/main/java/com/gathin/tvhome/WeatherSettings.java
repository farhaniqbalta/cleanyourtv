package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Remote-friendly weather controls and explicit city search. */
final class WeatherSettings {
    private final Activity activity;
    private final SharedPreferences choices;
    private final Runnable changed;
    private final WeatherStore store;
    private final TextView toggle, city, followTheme;
    private ExecutorService executor;
    private WeatherClient.Request request;
    private Future<?> task;
    private int generation;
    private boolean enabled, closed;

    WeatherSettings(Activity activity, SharedPreferences choices, boolean enabled, Runnable changed) {
        this.activity = activity;
        this.choices = choices;
        this.changed = changed;
        this.enabled = enabled;
        store = new WeatherStore(activity);
        toggle = activity.findViewById(R.id.weatherChoice);
        city = activity.findViewById(R.id.weatherCityChoice);
        followTheme = activity.findViewById(R.id.weatherFollowThemeChoice);
        toggle.setOnClickListener(v -> {
            this.enabled = !this.enabled;
            if (!this.enabled) cancelSearch();
            choices.edit().putBoolean("weather_enabled", this.enabled).apply();
            changed.run();
            refresh();
        });
        city.setOnClickListener(v -> searchDialog());
        followTheme.setOnClickListener(v -> {
            choices.edit().remove("weather_enabled").apply();
            this.enabled = Config.load(activity).weatherEnabled;
            if (!this.enabled) cancelSearch();
            changed.run();
            refresh();
        });
        refresh();
    }

    private void refresh() {
        toggle.setText(activity.getString(R.string.weather_toggle,
                activity.getString(enabled ? R.string.weather_on : R.string.weather_off)));
        WeatherClient.City selected = store.city();
        city.setText(activity.getString(R.string.weather_city, selected == null
                ? activity.getString(R.string.weather_city_missing) : selected.name));
        city.setVisibility(enabled ? View.VISIBLE : View.GONE);
        followTheme.setVisibility(choices.contains("weather_enabled") ? View.VISIBLE : View.GONE);
    }

    private void searchDialog() {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        new AlertDialog.Builder(activity).setTitle(R.string.weather_search).setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.weather_search_action, (dialog, which) -> search(input.getText().toString()))
                .show();
    }

    private void search(String query) {
        if (query.trim().isEmpty()) return;
        cancelSearch();
        executor = Executors.newSingleThreadExecutor();
        int currentGeneration = ++generation;
        WeatherClient.Request currentRequest = new WeatherClient.Request();
        request = currentRequest;
        task = executor.submit(() -> {
            List<WeatherClient.City> found;
            try { found = WeatherClient.search(query, currentRequest); }
            catch (Exception ignored) { found = Collections.emptyList(); }
            List<WeatherClient.City> results = found;
            activity.runOnUiThread(() -> {
                if (closed || activity.isFinishing() || currentGeneration != generation) return;
                request = null;
                task = null;
                if (results.isEmpty()) {
                    Toast.makeText(activity, R.string.weather_no_results, Toast.LENGTH_LONG).show();
                    return;
                }
                String[] names = new String[results.size()];
                for (int i = 0; i < names.length; i++) names[i] = results.get(i).name;
                new AlertDialog.Builder(activity).setTitle(R.string.weather_city_missing)
                        .setItems(names, (dialog, which) -> {
                            store.setCity(results.get(which));
                            changed.run();
                            refresh();
                        }).setNegativeButton(android.R.string.cancel, null).show();
            });
        });
    }

    void close() {
        closed = true;
        cancelSearch();
    }

    void cancelSearch() {
        generation++;
        if (request != null) request.cancel();
        if (task != null) task.cancel(true);
        if (executor != null) executor.shutdownNow();
        request = null;
        task = null;
        executor = null;
    }
}
