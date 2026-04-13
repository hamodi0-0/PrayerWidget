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
            new UpdateWidgetTask(context, appWidgetManager, appWidgetId).execute();
        }
    }

    private static class UpdateWidgetTask extends AsyncTask<Void, Void, PrayerTimeLogic.PrayerState> {
        private Context context;
        private AppWidgetManager appWidgetManager;
        private int appWidgetId;

        UpdateWidgetTask(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected PrayerTimeLogic.PrayerState doInBackground(Void... voids) {
            // Get cached prayer times (no API call)
            PrayerTimeManager manager = new PrayerTimeManager(context);
            String today = PrayerTimeManager.getTodayDateString();
            HashMap<String, String> prayerTimes = manager.getPrayerTimes(today);

            // Calculate state based on CURRENT time
            if (prayerTimes != null) {
                return PrayerTimeLogic.getPrayerState(prayerTimes);
            }
            return null;
        }

        @Override
        protected void onPostExecute(PrayerTimeLogic.PrayerState state) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            if (state != null) {
                if (state.isTimePassed) {
                    // For the "Time Passed" state, we stop the chronometer
                    // and just show the static text you already calculated
                    views.setChronometer(R.id.countdown_time, 0, null, false);
                    views.setTextViewText(R.id.prayer_name, state.currentPrayerName);
                    views.setTextViewText(R.id.prayer_time, "Passed");
                    views.setTextViewText(R.id.countdown_time, state.countdownDisplay);
                } else {
                    views.setTextViewText(R.id.prayer_name, state.nextPrayerName);
                    views.setTextViewText(R.id.prayer_time, state.nextPrayerTime);

                    // START THE REAL-TIME COUNTDOWN
                    long baseTime = PrayerTimeLogic.getChronometerBase(state.timeUntilNextSec);

                    // setChronometer(viewId, base, format, started)
                    views.setChronometer(R.id.countdown_time, baseTime, null, true);

                    // Set it to count down (Requires API 24+)
                    views.setChronometerCountDown(R.id.countdown_time, true);
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}