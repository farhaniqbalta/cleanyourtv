package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
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
        setPadding(0, dp(8), 0, 0);
        boolean rtl = getResources().getBoolean(R.bool.rtl_menu_bar);
        if (rtl) setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setVisibility(GONE);
        current = label(context, 15);
        current.setTypeface(UiFont.get(context, R.string.font_medium));
        if (rtl) current.setGravity(Gravity.RIGHT);
        current.setSingleLine(true);
        current.setEllipsize(TextUtils.TruncateAt.END);
        addView(current);
        forecast = new LinearLayout(context);
        forecast.setOrientation(HORIZONTAL);
        addView(forecast);
    }

    void show(WeatherClient.City city, WeatherClient.Snapshot snapshot) {
        int comma = city.name.indexOf(',');
        String shortCity = comma < 0 ? city.name : city.name.substring(0, comma);
        current.setText(Math.round(snapshot.temperature) + "° · " + condition(snapshot.code) + " · " + shortCity);
        forecast.removeAllViews();
        List<WeatherHours.Hour> hours = WeatherHours.next(snapshot.hours, Instant.now());
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(getContext());
        timeFormat.setTimeZone(TimeZone.getTimeZone(snapshot.timezone));
        for (WeatherHours.Hour hour : hours) {
            LinearLayout column = new LinearLayout(getContext());
            column.setOrientation(VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL);
            column.setPadding(0, dp(12), 0, 0);
            column.setFocusable(false);
            TextView time = label(getContext(), 12);
            time.setTextColor(Color.rgb(190, 190, 190));
            time.setSingleLine(true);
            time.setText(timeFormat.format(Date.from(hour.time)));
            column.addView(time);
            ImageView icon = new ImageView(getContext());
            icon.setImageResource(icon(hour.code));
            icon.setContentDescription(condition(hour.code));
            icon.setFocusable(false);
            LinearLayout.LayoutParams iconLayout = new LinearLayout.LayoutParams(dp(22), dp(22));
            iconLayout.topMargin = dp(5);
            column.addView(icon, iconLayout);
            TextView temperature = label(getContext(), 15);
            temperature.setTypeface(UiFont.get(getContext(), R.string.font_medium));
            temperature.setText(Math.round(hour.temperature) + "°");
            LinearLayout.LayoutParams temperatureLayout = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            temperatureLayout.topMargin = dp(4);
            column.addView(temperature, temperatureLayout);
            forecast.addView(column, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        }
        forecast.setVisibility(hours.size() == 4 ? VISIBLE : GONE);
        setVisibility(VISIBLE);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int available = MeasureSpec.getSize(widthSpec);
        forecast.getLayoutParams().width = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                ? dp(320) : Math.min(available, dp(320));
        super.onMeasure(widthSpec, heightSpec);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static TextView label(Context context, int size) {
        TextView text = new TextView(context);
        text.setTextColor(Color.rgb(220, 220, 220));
        text.setTextSize(size);
        text.setTypeface(UiFont.get(context, R.string.font_regular));
        text.setFocusable(false);
        text.setClickable(false);
        return text;
    }

    private static int icon(int code) {
        if (code == 0) return R.drawable.weather_sun;
        if (code > 0 && code <= 3) return R.drawable.weather_cloud;
        if (code == 45 || code == 48) return R.drawable.weather_fog;
        if (code >= 71 && code <= 77 || code >= 85 && code <= 86) return R.drawable.weather_snow;
        if (code >= 95 && code <= 99) return R.drawable.weather_storm;
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) return R.drawable.weather_rain;
        return R.drawable.weather_unknown;
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
