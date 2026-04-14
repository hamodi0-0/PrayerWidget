package com.abuhamza.prayerwidget;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView cityInput;
    private TextView currentLocText;
    private SharedPreferences prefs;
    private FusedLocationProviderClient fusedLocationClient;

    // Suggestions list
    private static final String[] CITY_SUGGESTIONS = new String[] {
            "Cairo, Egypt", "New Cairo City, Egypt", "Giza, Egypt", "Alexandria, Egypt",
            "Mecca, Saudi Arabia", "Medina, Saudi Arabia", "Riyadh, Saudi Arabia",
            "Dubai, UAE", "Abu Dhabi, UAE", "London, UK", "New York, USA",
            "Istanbul, Turkey", "Kuala Lumpur, Malaysia", "Jakarta, Indonesia"
    };

    // Permission Launcher
    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    fetchExactLocation();
                } else {
                    currentLocText.setText("Location permission denied");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize variables and UI elements
        prefs = getSharedPreferences("location_prefs", Context.MODE_PRIVATE);
        cityInput = findViewById(R.id.city_input);
        currentLocText = findViewById(R.id.current_location_text);
        Button saveBtn = findViewById(R.id.save_location_btn); // <-- Fixed saveBtn reference
        Button gpsBtn = findViewById(R.id.gps_location_btn);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 2. Setup AutoComplete Adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                CITY_SUGGESTIONS
        );
        cityInput.setAdapter(adapter);

        // 3. Load currently saved location for display
        String savedLoc = prefs.getString("display_location", "New Cairo City, Egypt");
        currentLocText.setText("Current: " + savedLoc);

        // 4. Button Listeners
        gpsBtn.setOnClickListener(v -> {
            currentLocText.setText("Finding location...");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchExactLocation();
            } else {
                locationPermissionRequest.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        saveBtn.setOnClickListener(v -> {
            String manualCity = cityInput.getText().toString().trim();
            if (!manualCity.isEmpty()) {
                saveLocationData("manual", manualCity, 0.0, 0.0);
            } else {
                Toast.makeText(this, "Please enter a city", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchExactLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            reverseGeocodeAndSave(location.getLatitude(), location.getLongitude());
                        } else {
                            currentLocText.setText("Could not get location. Turn on GPS.");
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void reverseGeocodeAndSave(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality() != null ? address.getLocality() : address.getSubAdminArea();
                String country = address.getCountryName();
                String displayLoc = city + ", " + country;

                cityInput.setText(displayLoc);
                saveLocationData("gps", displayLoc, lat, lng);
            }
        } catch (Exception e) {
            currentLocText.setText("Saved coords, but Geocoder failed.");
            saveLocationData("gps", "GPS Location", lat, lng);
        }
    }

    // Unified Save Method that handles UI, Data, and Widget Refresh
    private void saveLocationData(String inputMode, String displayLoc, double lat, double lng) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("input_mode", inputMode);
        editor.putString("display_location", displayLoc);
        editor.putFloat("lat", (float) lat);
        editor.putFloat("lng", (float) lng); // Changed to 'lng' to match logic
        editor.apply();

        currentLocText.setText("Current: " + displayLoc);
        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();

        // Clear prayer times cache so it forces a new API call
        PrayerTimeManager manager = new PrayerTimeManager(this);
        manager.clearCache();

        // Force Widget Refresh
        Intent intent = new Intent(this, PrayerWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), PrayerWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }
}