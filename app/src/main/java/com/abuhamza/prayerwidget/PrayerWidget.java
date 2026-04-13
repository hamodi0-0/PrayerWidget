package com.abuhamza.prayerwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.HashMap;

public class PrayerWidget extends AppWidgetProvider {
    private static final String TAG = "PrayerWidget";
    public static final String ACTION_EXACT_UPDATE = "com.abuhamza.prayerwidget.EXACT_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // Intercept our custom alarm trigger to refresh exactly on time
        if (ACTION_EXACT_UPDATE.equals(intent.getAction())) {
            Log.d(TAG, "Exact alarm triggered! Updating widget...");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, PrayerWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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
            PrayerTimeManager manager = new PrayerTimeManager(context);
            String today = PrayerTimeManager.getTodayDateString();
            HashMap<String, String> prayerTimes = manager.getPrayerTimes(today);

            if (prayerTimes != null) {
                return PrayerTimeLogic.getPrayerState(prayerTimes);
            }
            return null;
        }

        @Override
        protected void onPostExecute(PrayerTimeLogic.PrayerState state) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            if (state != null) {
                long nowMs = System.currentTimeMillis();
                long triggerAlarmInMs = 0; // When to fire the next exact update

                if (state.isTimePassed) {
                    // STATE: ELAPSED (Counting UP to 25 mins)
                    views.setTextViewText(R.id.prayer_name, state.currentPrayerName);
                    views.setTextViewText(R.id.prayer_time, "Passed");
                    views.setTextViewText(R.id.status_label, "Time passed");

                    // System up-time minus the seconds that have already passed
                    long base = SystemClock.elapsedRealtime() - (state.timeUntilNextSec * 1000);
                    views.setChronometer(R.id.countdown_time, base, null, true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        views.setChronometerCountDown(R.id.countdown_time, false); // Count UP
                    }

                    // Schedule update for when the 25 minutes are over
                    long twentyFiveMinsMs = 25 * 60 * 1000;
                    triggerAlarmInMs = twentyFiveMinsMs - (state.timeUntilNextSec * 1000);

                } else {
                    // STATE: UPCOMING (Counting DOWN to Adhan)
                    views.setTextViewText(R.id.prayer_name, state.nextPrayerName);
                    views.setTextViewText(R.id.prayer_time, state.nextPrayerTime);
                    views.setTextViewText(R.id.status_label, "Time for Adhan");

                    // System up-time plus the seconds remaining
                    long base = SystemClock.elapsedRealtime() + (state.timeUntilNextSec * 1000);
                    views.setChronometer(R.id.countdown_time, base, null, true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        views.setChronometerCountDown(R.id.countdown_time, true); // Count DOWN
                    }

                    // Schedule exact alarm to fire when countdown hits 00:00:00
                    triggerAlarmInMs = state.timeUntilNextSec * 1000;
                }

                scheduleNextUpdate(nowMs + triggerAlarmInMs);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        private void scheduleNextUpdate(long triggerTimeAbsoluteMs) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, PrayerWidget.class);
            intent.setAction(ACTION_EXACT_UPDATE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                // Handling the SecurityException warning from your screenshot
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                    } else {
                        // Fallback if the user revoked exact alarm permission
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Exact alarm permission missing", e);
                // Safe fallback to prevent crash
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
            }
        }
    }
}