package com.gathin.tvhome;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONObject;

/** Schedules weather only while Home is visible and rejects callbacks after pause. */
final class WeatherController {
    private static final long REFRESH = 30 * 60_000L, RETRY = 5 * 60_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor;
    private final WeatherStore store;
    private final WeatherView view;
    private Future<?> task;
    private WeatherClient.Request request;
    private volatile boolean active;
    private volatile int generation;
    private WeatherClient.City city;
    private WeatherClient.Snapshot cached;
    private long fetched;
    private long retryAt;
    private final Runnable refresh = this::load;
    private final Runnable updateDisplay = new Runnable() {
        @Override public void run() {
            if (!active) return;
            render();
            handler.postDelayed(this, 60_000L);
        }
    };

    WeatherController(WeatherView view) {
        this.view = view;
        store = new WeatherStore(view.getContext());
    }

    void resume() {
        if (active) return;
        active = true;
        executor = Executors.newSingleThreadExecutor();
        view.setVisibility(View.GONE);
        int requestGeneration = ++generation;
        task = executor.submit(() -> {
            long now = System.currentTimeMillis();
            WeatherClient.City storedCity = store.city();
            WeatherClient.Snapshot storedSnapshot = store.snapshot(now);
            long storedFetched = store.fetched();
            handler.post(() -> {
                if (!active || requestGeneration != generation) return;
                task = null;
                city = storedCity;
                cached = storedSnapshot;
                fetched = storedFetched;
                updateDisplay.run();
                if (city == null) return;
                long age = System.currentTimeMillis() - fetched;
                if (cached == null || age < 0 || age >= REFRESH) load();
                else handler.postDelayed(refresh, REFRESH - age);
            });
        });
    }

    private void render() {
        long now = System.currentTimeMillis();
        long age = now - fetched;
        if (now >= MainActivity.TIME_VALID_AFTER_MS && city != null && cached != null
                && age >= 0 && age <= 6 * 60 * 60_000L && cached.reasonableAt(now)) view.show(city, cached);
        else view.setVisibility(View.GONE);
    }

    private void load() {
        if (!active || task != null && !task.isDone()) return;
        long retryDelay = retryAt - SystemClock.elapsedRealtime();
        if (retryDelay > 0) {
            handler.postDelayed(refresh, retryDelay);
            return;
        }
        if (System.currentTimeMillis() < MainActivity.TIME_VALID_AFTER_MS) {
            handler.postDelayed(refresh, RETRY);
            return;
        }
        if (city == null) return;
        WeatherClient.City selectedCity = city;
        int requestGeneration = ++generation;
        WeatherClient.Request currentRequest = new WeatherClient.Request();
        request = currentRequest;
        task = executor.submit(() -> {
            JSONObject json = null;
            WeatherClient.Snapshot snapshot = null;
            try {
                json = WeatherClient.fetch(selectedCity.latitude, selectedCity.longitude, currentRequest);
                snapshot = WeatherClient.parse(json);
                if (!snapshot.reasonableAt(System.currentTimeMillis())) snapshot = null;
            } catch (Exception ignored) { }
            long received = System.currentTimeMillis();
            if (snapshot != null && active && requestGeneration == generation) {
                try { store.save(json, received); }
                catch (RuntimeException ignored) { /* A cache write failure must not stop Home. */ }
            }
            WeatherClient.Snapshot data = snapshot;
            handler.post(() -> {
                if (!active || requestGeneration != generation) return;
                task = null;
                request = null;
                if (data != null) {
                    cached = data;
                    fetched = received;
                }
                retryAt = data == null ? SystemClock.elapsedRealtime() + RETRY : 0;
                render();
                handler.postDelayed(refresh, data == null ? RETRY : REFRESH);
            });
        });
    }

    void pause() {
        active = false;
        generation++;
        handler.removeCallbacks(refresh);
        handler.removeCallbacks(updateDisplay);
        if (request != null) request.cancel();
        request = null;
        if (task != null) task.cancel(true);
        task = null;
        if (executor != null) executor.shutdownNow();
        executor = null;
    }

    void close() {
        pause();
    }
}
