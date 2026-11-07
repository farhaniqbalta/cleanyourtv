package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/** Small non-interactive weather readout below the clock. */
final class WeatherView extends LinearLayout {
    private final TextView current;
    private final LinearLayout forecast;

    WeatherView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setFocusable(false);
        setFocusableInTouchMode(false);
        boolean rtl = getResources().getBoolean(R.bool.rtl_menu_bar);
        if (rtl) setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setVisibility(GONE);
        current = label(context, 15);
        if (rtl) current.setGravity(Gravity.RIGHT);
        current.setSingleLine(true);
        current.setEllipsize(TextUtils.TruncateAt.END);
        addView(current);
        forecast = new LinearLayout(context);
        forecast.setOrientation(HORIZONTAL);
        addView(forecast);
    }

    void show(WeatherClient.City city, WeatherClient.Snapshot snapshot) {
        current.setText(city.name + " · " + Math.round(snapshot.temperature) + "° · " + condition(snapshot.code));
        forecast.removeAllViews();
        List<WeatherHours.Hour> hours = WeatherHours.next(snapshot.hours, Instant.now());
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(getContext());
        timeFormat.setTimeZone(TimeZone.getTimeZone(snapshot.timezone));
        for (WeatherHours.Hour hour : hours) {
            TextView text = label(getContext(), 13);
            text.setText(timeFormat.format(Date.from(hour.time))
                    + "\n" + symbol(hour.code) + "\n" + Math.round(hour.temperature) + "°");
            text.setPadding(0, Math.round(8 * getResources().getDisplayMetrics().density), 0, 0);
            forecast.addView(text, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        }
        forecast.setVisibility(hours.size() == 4 ? VISIBLE : GONE);
        setVisibility(VISIBLE);
    }

    private static TextView label(Context context, int size) {
        TextView text = new TextView(context);
        text.setTextColor(Color.rgb(220, 220, 220));
        text.setTextSize(size);
        text.setFocusable(false);
        text.setClickable(false);
        return text;
    }

    private static String symbol(int code) {
        if (code == 0) return "☀︎";
        if (code > 0 && code <= 3) return "☁︎";
        if (code == 45 || code == 48) return "≋";
        if (code >= 71 && code <= 77 || code >= 85 && code <= 86) return "❄︎";
        if (code >= 95 && code <= 99) return "ϟ";
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) return "☂︎";
        return "·";
    }

    private String condition(int code) {
        int label = R.string.weather_unknown;
        if (code == 0) label = R.string.weather_clear;
        else if (code > 0 && code <= 3) label = R.string.weather_cloudy;
        else if (code == 45 || code == 48) label = R.string.weather_fog;
        else if (code >= 71 && code <= 77 || code >= 85 && code <= 86) label = R.string.weather_snow;
        else if (code >= 95 && code <= 99) label = R.string.weather_thunderstorm;
        else if (code >= 51 && code <= 67 || code >= 80 && code <= 82) label = R.string.weather_rain;
        return getContext().getString(label);
    }
}
