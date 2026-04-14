package com.abuhamza.prayerwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PrayerTimeManager {
    private static final String TAG = "PrayerTimeManager";
    private static final String PREFS_NAME = "prayer_times_cache";
    private static final String CACHE_KEY = "prayer_times_";
    private static final int METHOD = 5; // Egyptian General Authority method

    private Context context;
    private SharedPreferences cachePrefs;
    private OkHttpClient httpClient;

    public PrayerTimeManager(Context context) {
        this.context = context;
        this.cachePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient();
    }

    public HashMap<String, String> getPrayerTimes(String dateString) {
        String cachedData = cachePrefs.getString(CACHE_KEY + dateString, null);
        if (cachedData != null) {
            Log.d(TAG, "Using cached prayer times for " + dateString);
            return parsePrayerTimesJson(cachedData);
        }

        Log.d(TAG, "Fetching prayer times for " + dateString + " from API");
        return fetchFromAPI(dateString);
    }

    private HashMap<String, String> fetchFromAPI(String dateString) {
        try {
            SharedPreferences locPrefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE);
            String inputMode = locPrefs.getString("input_mode", "manual");
            String url;

            if ("gps".equals(inputMode)) {
                // GPS Mode: Use precise Latitude and Longitude
                float lat = locPrefs.getFloat("lat", 30.0056f);
                float lng = locPrefs.getFloat("lng", 31.4778f);
                url = String.format(Locale.US,
                        "https://api.aladhan.com/v1/timings/%s?latitude=%f&longitude=%f&method=%d",
                        dateString, lat, lng, METHOD
                );
            } else {
                // Manual Text Mode: Use City and Country
                String displayLoc = locPrefs.getString("display_location", "New Cairo City, Egypt");
                String[] parts = displayLoc.split(",");
                String city = parts[0].trim();
                String country = parts.length > 1 ? parts[1].trim() : "";

                // URL encode the strings just in case there are spaces (e.g. "New York")
                String encodedCity = URLEncoder.encode(city, "UTF-8");
                String encodedCountry = URLEncoder.encode(country, "UTF-8");

                url = String.format(Locale.US,
                        "https://api.aladhan.com/v1/timingsByCity/%s?city=%s&country=%s&method=%d",
                        dateString, encodedCity, encodedCountry, METHOD
                );
            }

            Log.d(TAG, "Requesting URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                cachePrefs.edit().putString(CACHE_KEY + dateString, responseBody).apply();
                Log.d(TAG, "Prayer times cached for " + dateString);
                return parsePrayerTimesJson(responseBody);
            } else {
                Log.e(TAG, "API Error: " + response.code());
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage());
            return null;
        }
    }

    private HashMap<String, String> parsePrayerTimesJson(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);

            if (response == null || !response.has("data")) {
                Log.e(TAG, "Invalid API response");
                return null;
            }

            JsonObject timings = response.getAsJsonObject("data").getAsJsonObject("timings");

            HashMap<String, String> prayerTimes = new HashMap<>();
            prayerTimes.put("Fajr", timings.get("Fajr").getAsString().substring(0, 5));
            prayerTimes.put("Shurouk", timings.get("Sunrise").getAsString().substring(0, 5));
            prayerTimes.put("Dhuhr", timings.get("Dhuhr").getAsString().substring(0, 5));
            prayerTimes.put("Asr", timings.get("Asr").getAsString().substring(0, 5));
            prayerTimes.put("Maghrib", timings.get("Maghrib").getAsString().substring(0, 5));
            prayerTimes.put("Isha", timings.get("Isha").getAsString().substring(0, 5));

            Log.d(TAG, "Parsed prayer times: " + prayerTimes.toString());
            return prayerTimes;

        } catch (Exception e) {
            Log.e(TAG, "Parsing error: " + e.getMessage());
            return null;
        }
    }

    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        return sdf.format(new Date());
    }

    public void clearCache() {
        cachePrefs.edit().clear().apply();
        Log.d(TAG, "Cache cleared");
    }
}