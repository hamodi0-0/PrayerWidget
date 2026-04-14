package com.abuhamza.prayerwidget;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvMainTimer, tvStatusLabel, tvGregorian, tvHijri;
    private LinearLayout prayerListContainer;
    private HashMap<String, String> currentTimes;

    // Core logic variables
    private String selectedPrayerKey = "Fajr";
    private final String[] PRAYER_KEYS = {"Fajr", "Shurouk", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private final String[] PRAYER_NAMES = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private View[] rowViews = new View[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Binding
        tvMainTimer = findViewById(R.id.tv_main_timer);
        tvStatusLabel = findViewById(R.id.tv_status_label);
        tvGregorian = findViewById(R.id.tv_gregorian_date);
        tvHijri = findViewById(R.id.tv_hijri_date);
        prayerListContainer = findViewById(R.id.prayer_list_container);

        // Location Button
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LocationActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDataAndBuildUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable); // Stop timer when app is in background
    }

    private void loadDataAndBuildUI() {
        PrayerTimeManager manager = new PrayerTimeManager(this);
        // Using a background thread is better, but keeping it synchronous here for simplicity if OkHttp call is quick.
        // If it crashes due to NetworkOnMainThread, wrap this in an ExecutorService or Thread.
        new Thread(() -> {
            currentTimes = manager.getPrayerTimes(PrayerTimeManager.getTodayDateString());
            runOnUiThread(() -> {
                if (currentTimes != null) {
                    tvGregorian.setText(currentTimes.get("GregorianDate"));
                    tvHijri.setText(currentTimes.get("HijriDate"));
                    buildTable();
                    autoSelectNextPrayer();
                    startTimer();
                } else {
                    Toast.makeText(this, "Failed to load times. Set location.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void buildTable() {
        prayerListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < PRAYER_KEYS.length; i++) {
            final String key = PRAYER_KEYS[i];
            final String displayName = PRAYER_NAMES[i];

            View row = inflater.inflate(R.layout.item_prayer_row, prayerListContainer, false);
            TextView tvName = row.findViewById(R.id.row_name);
            TextView tvTime = row.findViewById(R.id.row_time);

            tvName.setText(displayName);
            tvTime.setText(currentTimes.get(key));

            // Click listener for seamless transition
            row.setOnClickListener(v -> selectPrayer(key));

            rowViews[i] = row;
            prayerListContainer.addView(row);
        }
    }

    private void autoSelectNextPrayer() {
        long now = System.currentTimeMillis();
        selectedPrayerKey = "Isha"; // Default if all passed

        for (String key : PRAYER_KEYS) {
            if (getPrayerTimeMillis(currentTimes.get(key)) > now) {
                selectedPrayerKey = key;
                break;
            }
        }
        selectPrayer(selectedPrayerKey);
    }

    private void selectPrayer(String key) {
        selectedPrayerKey = key;

        // Update Highlights
        for (int i = 0; i < PRAYER_KEYS.length; i++) {
            if (PRAYER_KEYS[i].equals(key)) {
                rowViews[i].setBackgroundColor(Color.parseColor("#27272A"));
            } else {
                rowViews[i].setBackgroundColor(Color.TRANSPARENT);
            }
        }

        // Force an immediate UI update so it doesn't wait for the next second tick
        updateTimerUI();
    }

    private void startTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentTimes != null) {
                updateTimerUI();
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    private void updateTimerUI() {
        String timeStr = currentTimes.get(selectedPrayerKey);
        if (timeStr == null) return;

        long targetMillis = getPrayerTimeMillis(timeStr);
        long now = System.currentTimeMillis();
        long diff = targetMillis - now;

        if (diff > 0) {
            // Countdown (Not passed yet)
            tvStatusLabel.setText("Time left for " + getDisplayName(selectedPrayerKey));
            tvMainTimer.setText(formatDuration(diff));
        } else {
            // Count-up (Passed)
            tvStatusLabel.setText(getDisplayName(selectedPrayerKey) + " Passed");
            tvMainTimer.setText(formatDuration(-diff)); // Pass absolute value
        }
    }

    // --- Helper Methods ---

    private long getPrayerTimeMillis(String timeString) {
        // timeString format from Aladhan is usually "HH:mm"
        Calendar cal = Calendar.getInstance();
        String[] parts = timeString.split(":");
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    private String formatDuration(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60)) % 24;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String getDisplayName(String key) {
        for (int i = 0; i < PRAYER_KEYS.length; i++) {
            if (PRAYER_KEYS[i].equals(key)) return PRAYER_NAMES[i];
        }
        return key;
    }
}