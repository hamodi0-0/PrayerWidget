package com.abuhamza.prayerwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
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

    /**
     * Fetch prayer times for a specific date from API
     * Returns cached data if available, otherwise fetches from API
     */
    public HashMap<String, String> getPrayerTimes(String dateString) {
        // First, check if we have cached data for this date
        String cachedData = cachePrefs.getString(CACHE_KEY + dateString, null);
        if (cachedData != null) {
            Log.d(TAG, "Using cached prayer times for " + dateString);
            return parsePrayerTimesJson(cachedData);
        }

        // If not cached, fetch from API
        Log.d(TAG, "Fetching prayer times for " + dateString + " from API");
        return fetchFromAPI(dateString);
    }

    /**
     * Fetch prayer times from Aladhan API using saved location
     */
    private HashMap<String, String> fetchFromAPI(String dateString) {
        try {
            // Retrieve coordinates from the location_prefs file
            SharedPreferences locPrefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE);

            // Default to New Cairo coordinates if none are saved
            float lat = locPrefs.getFloat("lat", 30.0056f);
            float lon = locPrefs.getFloat("lon", 31.4778f);

            String url = String.format(Locale.US,
                    "https://api.aladhan.com/v1/timings/%s?latitude=%f&longitude=%f&method=%d",
                    dateString, lat, lon, METHOD
            );

            Log.d(TAG, "Requesting URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();

                // Cache the response
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

    /**
     * Parse the JSON response from Aladhan API
     */
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

    /**
     * Get today's date in DD-MM-YYYY format for API
     */
    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Clear all cached prayer times
     */
    public void clearCache() {
        cachePrefs.edit().clear().apply();
        Log.d(TAG, "Cache cleared");
    }
}