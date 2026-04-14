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
        // Just trigger the task for each widget
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
// This is the views object that actually goes to the screen
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // --- NEW CODE: Attach the click listener here ---
            Intent intent = new Intent(context, MainActivity.class);
            // This flag is best practice when launching from a widget
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
            if (state != null) {
                long nowMs = System.currentTimeMillis();
                long triggerAlarmInMs = 0;

                if (state.isTimePassed) {
                    // STATE: ELAPSED (Counting UP to 25 mins)
                    views.setTextViewText(R.id.prayer_name, state.currentPrayerName);

                    // THIS IS THE FIX: Show the actual time instead of "Passed"
                    views.setTextViewText(R.id.prayer_time, state.currentPrayerTime);

                    views.setTextViewText(R.id.status_label, "Time passed");

                    long base = SystemClock.elapsedRealtime() - (state.timeUntilNextSec * 1000);
                    views.setChronometer(R.id.countdown_time, base, null, true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        views.setChronometerCountDown(R.id.countdown_time, false);
                    }

                    long twentyFiveMinsMs = 25 * 60 * 1000;
                    triggerAlarmInMs = twentyFiveMinsMs - (state.timeUntilNextSec * 1000);

                } else {
                    // STATE: UPCOMING (Counting DOWN to Adhan)
                    views.setTextViewText(R.id.prayer_name, state.nextPrayerName);
                    views.setTextViewText(R.id.prayer_time, state.nextPrayerTime);
                    views.setTextViewText(R.id.status_label, "Time for Adhan");

                    long base = SystemClock.elapsedRealtime() + (state.timeUntilNextSec * 1000);
                    views.setChronometer(R.id.countdown_time, base, null, true);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        views.setChronometerCountDown(R.id.countdown_time, true);
                    }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Exact alarm permission missing", e);
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeAbsoluteMs, pendingIntent);
            }
        }
    }
}