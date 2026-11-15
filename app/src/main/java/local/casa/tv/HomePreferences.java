package local.casa.tv;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class HomePreferences {
    final String signature;
    final String city;
    final double latitude;
    final double longitude;

    private HomePreferences(Properties values) {
        signature = values.getProperty("signature", "").trim();
        city = values.getProperty("weather.city", "").trim();
        latitude = coordinate(values.getProperty("weather.latitude"), 90);
        longitude = coordinate(values.getProperty("weather.longitude"), 180);
    }

    static HomePreferences read(Context context) {
        Properties values = new Properties();
        File directory = context.getExternalFilesDir(null);
        File config = directory == null ? null : new File(directory, "casa.properties");
        if (config != null && config.isFile()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(config), StandardCharsets.UTF_8)) {
                values.load(reader);
            } catch (IOException | IllegalArgumentException error) {
                Log.w("CasaTV", "Não foi possível ler as preferências", error);
                values.clear();
            }
        }
        return new HomePreferences(values);
    }

    boolean hasWeather() {
        return !city.isEmpty() && Double.isFinite(latitude) && Double.isFinite(longitude);
    }

    private static double coordinate(String value, int limit) {
        if (value == null) return Double.NaN;
        try {
            double number = Double.parseDouble(value.trim());
            return Math.abs(number) <= limit ? number : Double.NaN;
        } catch (NumberFormatException error) {
            return Double.NaN;
        }
    }
}
