package com.abuhamza.prayerwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

public class PrayerWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        String prayerTime = "06:26";
        String prayerName = "Maghrib";
        String countdownTime = "1:56:25";

        views.setTextViewText(R.id.prayer_time, prayerTime);
        views.setTextViewText(R.id.prayer_name, prayerName);
        views.setTextViewText(R.id.countdown_time, countdownTime);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}