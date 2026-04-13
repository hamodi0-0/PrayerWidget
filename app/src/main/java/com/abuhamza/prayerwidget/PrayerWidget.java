package com.abuhamza.prayerwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.HashMap;

public class PrayerWidget extends AppWidgetProvider {
    private static final String TAG = "PrayerWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called");

        for (int appWidgetId : appWidgetIds) {
            // Fetch in background thread
            new FetchPrayerTimesTask(context, appWidgetManager, appWidgetId).execute();
        }
    }

    /**
     * Background task to fetch prayer times without blocking main thread
     */
    private static class FetchPrayerTimesTask extends AsyncTask<Void, Void, HashMap<String, String>> {
        private Context context;
        private AppWidgetManager appWidgetManager;
        private int appWidgetId;

        FetchPrayerTimesTask(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected HashMap<String, String> doInBackground(Void... voids) {
            PrayerTimeManager manager = new PrayerTimeManager(context);
            String today = PrayerTimeManager.getTodayDateString();

            Log.d(TAG, "Fetching prayer times for: " + today);
            return manager.getPrayerTimes(today);
        }

        @Override
        protected void onPostExecute(HashMap<String, String> prayerTimes) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            if (prayerTimes != null) {
                Log.d(TAG, "Successfully fetched prayer times: " + prayerTimes.toString());

                String fajr = prayerTimes.get("Fajr");
                String dhuhr = prayerTimes.get("Dhuhr");
                String maghrib = prayerTimes.get("Maghrib");

                views.setTextViewText(R.id.prayer_name, "Fajr");
                views.setTextViewText(R.id.prayer_time, fajr);
                views.setTextViewText(R.id.countdown_time, "Test: " + dhuhr + " / " + maghrib);

            } else {
                Log.e(TAG, "Failed to fetch prayer times");
                views.setTextViewText(R.id.prayer_name, "Error");
                views.setTextViewText(R.id.prayer_time, "N/A");
                views.setTextViewText(R.id.countdown_time, "API failed");
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}