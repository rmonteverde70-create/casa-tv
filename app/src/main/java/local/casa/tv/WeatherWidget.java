package local.casa.tv;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class WeatherWidget extends LinearLayout {
    private static final String FORECAST_ENDPOINT = "https://api.open-meteo.com/v1/forecast";
    private static final long UPDATE_INTERVAL = 30 * 60 * 1000L;
    private static final long RETRY_INTERVAL = 5 * 60 * 1000L;
    private static final Executor WORKER = Executors.newSingleThreadExecutor();

    private final String forecastUrl;
    private final TextView temperature;
    private final TextView conditions;
    private final TextView range;
    private final Runnable refresh = this::refreshForecast;
    private boolean loading;
    private Forecast lastForecast;
    private long nextRefresh;

    WeatherWidget(Context context, HomePreferences location) {
        super(context);
        forecastUrl = FORECAST_ENDPOINT + "?latitude=" + location.latitude
                + "&longitude=" + location.longitude
                + "&current=temperature_2m,weather_code,is_day"
                + "&daily=temperature_2m_min,temperature_2m_max&forecast_days=1&timezone=auto";
        setOrientation(VERTICAL);
        setPadding(dp(13), dp(9), dp(13), dp(8));
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(0xff0c0c0c);
        panel.setCornerRadius(dp(14));
        panel.setStroke(dp(1), 0xff292929);
        setBackground(panel);
        addText(location.city, 12, 0xffd8d5cf);
        temperature = addText("— °C", 27, 0xfff5f2ec);
        temperature.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        temperature.setIncludeFontPadding(false);
        conditions = addText("Consultando o tempo…", 11, 0xffd8d5cf);
        range = addText("", 11, 0xffb8b5b0);
        addText("Open-Meteo", 9, 0xffaaa6a0);
    }

    private TextView addText(String value, int size, int color) {
        TextView label = new TextView(getContext());
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        addView(label);
        return label;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        scheduleRefresh();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (refresh != null) scheduleRefresh();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(refresh);
        super.onDetachedFromWindow();
    }

    private void scheduleRefresh() {
        removeCallbacks(refresh);
        if (!loading && isAttachedToWindow() && getWindowVisibility() == View.VISIBLE) {
            postDelayed(refresh, Math.max(0L, nextRefresh - android.os.SystemClock.elapsedRealtime()));
        }
    }

    private void refreshForecast() {
        loading = true;
        WORKER.execute(() -> {
            Forecast forecast = fetch();
            post(() -> {
                loading = false;
                nextRefresh = android.os.SystemClock.elapsedRealtime()
                        + (forecast == null ? RETRY_INTERVAL : UPDATE_INTERVAL);
                if (forecast != null) {
                    lastForecast = forecast;
                    temperature.setText(forecast.temperature + " °C");
                    conditions.setText(forecast.conditions);
                    range.setText("Mín. " + forecast.low + "°  ·  Máx. " + forecast.high + "°");
                } else if (lastForecast == null) {
                    conditions.setText("Tempo indisponível");
                } else {
                    conditions.setText("Última atualização");
                }
                scheduleRefresh();
            });
        });
    }

    private Forecast fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(forecastUrl).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            try (InputStream stream = connection.getInputStream();
                 ByteArrayOutputStream data = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int count;
                while ((count = stream.read(buffer)) != -1) data.write(buffer, 0, count);
                JSONObject response = new JSONObject(new String(data.toByteArray(), StandardCharsets.UTF_8));
                JSONObject current = response.getJSONObject("current");
                JSONObject daily = response.getJSONObject("daily");
                return new Forecast(
                        Math.round(current.getDouble("temperature_2m")),
                        Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)),
                        Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)),
                        weatherLabel(current.getInt("weather_code"), current.getInt("is_day") == 1));
            }
        } catch (Exception error) {
            android.util.Log.w("CasaTV", "Consulta meteorológica indisponível", error);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String weatherLabel(int code, boolean day) {
        if (code == 0) return day ? "Ensolarado" : "Céu limpo";
        if (code == 1 || code == 2) return "Parcialmente nublado";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Neblina";
        if (code >= 51 && code <= 57) return "Garoa";
        if (code >= 61 && code <= 67) return "Chuva";
        if (code >= 71 && code <= 77) return "Neve";
        if (code >= 80 && code <= 82) return "Pancadas de chuva";
        if (code == 85 || code == 86) return "Pancadas de neve";
        if (code >= 95) return "Trovoadas";
        return "Condições variáveis";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Forecast {
        final long temperature;
        final long low;
        final long high;
        final String conditions;

        Forecast(long temperature, long low, long high, String conditions) {
            this.temperature = temperature;
            this.low = low;
            this.high = high;
            this.conditions = conditions;
        }
    }
}
