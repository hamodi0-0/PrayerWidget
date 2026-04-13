package com.abuhamza.prayerwidget;

import android.util.Log;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PrayerTimeLogic {
    private static final String TAG = "PrayerTimeLogic";

    // Prayer periods in order
    private static final List<String> PRAYER_ORDER = new ArrayList<String>() {{
        add("Fajr");
        add("Shurouk");
        add("Dhuhr");
        add("Asr");
        add("Maghrib");
        add("Isha");
    }};

    private static final long TWENTY_FIVE_MINUTES_S = 25 * 60;

    public static class PrayerState {
        public String currentPrayerName;
        public String nextPrayerName;
        public String nextPrayerTime;
        public String countdownDisplay;
        public long timeUntilNextSec;
        public boolean isTimePassed;

        @Override
        public String toString() {
            return "PrayerState{" +
                    "nextPrayerName='" + nextPrayerName + '\'' +
                    ", nextPrayerTime='" + nextPrayerTime + '\'' +
                    ", countdownDisplay='" + countdownDisplay + '\'' +
                    ", isTimePassed=" + isTimePassed +
                    '}';
        }
    }

    public static PrayerState getPrayerState(java.util.HashMap<String, String> prayerTimes) {
        PrayerState state = new PrayerState();

        if (prayerTimes == null || prayerTimes.isEmpty()) {
            Log.e(TAG, "Prayer times not available");
            state.nextPrayerName = "Error";
            state.countdownDisplay = "No data";
            return state;
        }

        try {
            // Get current time in minutes since midnight
            Calendar now = Calendar.getInstance();
            int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int currentSeconds = now.get(Calendar.SECOND);
            long currentTotalSeconds = currentMinutes * 60L + currentSeconds;

            // Parse all prayer times
            List<PrayerTimeInfo> allPrayers = new ArrayList<>();
            for (String prayerName : PRAYER_ORDER) {
                String timeStr = prayerTimes.get(prayerName);
                if (timeStr != null) {
                    int prayerMinutes = parseTimeStringToMinutes(timeStr);
                    allPrayers.add(new PrayerTimeInfo(prayerName, timeStr, prayerMinutes));
                }
            }

            // Find current and next prayer
            PrayerTimeInfo currentPrayer = null;
            PrayerTimeInfo nextPrayer = null;

            for (PrayerTimeInfo prayer : allPrayers) {
                if (prayer.minutesSinceMidnight <= currentMinutes) {
                    currentPrayer = prayer;
                } else {
                    nextPrayer = prayer;
                    break;
                }
            }

            // If no next prayer found (past Isha), next is Fajr tomorrow
            if (nextPrayer == null && !allPrayers.isEmpty()) {
                nextPrayer = allPrayers.get(0);
            }

            if (nextPrayer == null) {
                state.nextPrayerName = "Error";
                state.countdownDisplay = "No prayers";
                return state;
            }

            // Check if in "Time passed" state (within 25 minutes of current prayer)
            if (currentPrayer != null) {
                long secondsSinceCurrent = currentTotalSeconds - (currentPrayer.minutesSinceMidnight * 60L);

                if (secondsSinceCurrent >= 0 && secondsSinceCurrent < TWENTY_FIVE_MINUTES_S) {
                    state.currentPrayerName = currentPrayer.name;
                    state.isTimePassed = true;

                    // THIS IS THE CRUCIAL LINE ADDED FOR THE CHRONOMETER
                    state.timeUntilNextSec = secondsSinceCurrent;

                    state.countdownDisplay = formatSeconds(secondsSinceCurrent);
                    return state;
                }
            }

            // Normal countdown state
            long secondsUntilNext;
            if (nextPrayer.minutesSinceMidnight > currentMinutes) {
                // Prayer is today
                secondsUntilNext = (nextPrayer.minutesSinceMidnight * 60L) - currentTotalSeconds;
            } else {
                // Prayer is tomorrow
                long secondsUntilMidnight = (24 * 60 * 60) - currentTotalSeconds;
                secondsUntilNext = secondsUntilMidnight + (nextPrayer.minutesSinceMidnight * 60L);
            }

            state.isTimePassed = false;
            state.nextPrayerName = nextPrayer.name;
            state.nextPrayerTime = nextPrayer.timeStr;
            state.countdownDisplay = formatSeconds(secondsUntilNext);
            state.timeUntilNextSec = secondsUntilNext;

            return state;

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            e.printStackTrace();
            state.nextPrayerName = "Error";
            state.countdownDisplay = "Error";
            return state;
        }
    }

    private static int parseTimeStringToMinutes(String timeStr) throws Exception {
        String[] parts = timeStr.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours * 60 + minutes;
    }

    private static String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
    }

    private static class PrayerTimeInfo {
        String name;
        String timeStr;
        int minutesSinceMidnight;

        PrayerTimeInfo(String name, String timeStr, int minutesSinceMidnight) {
            this.name = name;
            this.timeStr = timeStr;
            this.minutesSinceMidnight = minutesSinceMidnight;
        }
    }
}