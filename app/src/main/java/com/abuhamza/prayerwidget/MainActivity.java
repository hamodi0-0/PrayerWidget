package com.abuhamza.prayerwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText cityInput;
    private TextView currentLocText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This links to res/layout/activity_main.xml
        setContentView(R.layout.activity_main);

        cityInput = findViewById(R.id.city_input);
        currentLocText = findViewById(R.id.current_location_text);
        Button saveBtn = findViewById(R.id.save_location_btn);

        prefs = getSharedPreferences("location_prefs", MODE_PRIVATE);

        String savedCity = prefs.getString("city_name", "New Cairo City");
        currentLocText.setText("Current: " + savedCity);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityInput.getText().toString().trim();
                if (!city.isEmpty()) {
                    updateLocation(city);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateLocation(String cityName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(cityName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double lat = address.getLatitude();
                double lon = address.getLongitude();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putFloat("lat", (float) lat);
                editor.putFloat("lon", (float) lon);
                editor.putString("city_name", cityName);
                editor.apply();

                // Clear prayer times cache
                PrayerTimeManager manager = new PrayerTimeManager(this);
                manager.clearCache();

                currentLocText.setText("Current: " + cityName);
                Toast.makeText(this, "Location updated: " + cityName, Toast.LENGTH_SHORT).show();

                // Force Widget Refresh
                Intent intent = new Intent(this, PrayerWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(getApplication())
                        .getAppWidgetIds(new ComponentName(getApplication(), PrayerWidget.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);

            } else {
                Toast.makeText(this, "City not found. Try adding a country.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}