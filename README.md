 <p align="center">
      <img src="assets/PrayerWidgetLogoRounded.png" width="120" height="120" alt="Prayer Widget Logo">
      <h1 align="center">Prayer Times Widget</h1>
      <p align="center">
        <strong>A modern, dark-themed Android widget for tracking Islamic prayer times.</strong>
      </p>
 </p>
---

## Overview

The application utilizes the Aladhan API to fetch prayer timings and displays them through a modern, dark-themed user interface. The primary focus is a functional home screen widget that provides real-time updates on prayer schedules.

### Core Functionality
* **Real-time Countdown**: The widget displays a live countdown to the next upcoming prayer time.
* **Elapsed Time Tracking**: Upon reaching a prayer time, the widget transitions to a "Passed" state, tracking the time elapsed since the prayer began.
* **Location Customization**: Users can set their location using a searchable input field.
* **Egypt-Specific Optimization**: The app includes a comprehensive list of Egyptian cities and districts while supporting manual entry for any global "City, Country" combination.
* **Modern Interface**: The interface utilizes high-contrast zinc tones and rounded components for clarity and scannability.

---

## Installation Instructions

To install the application on an Android device using the APK file, follow these steps:

### 1. Enable Installation from Unknown Sources
By default, Android restricts installations from sources other than the Google Play Store. 
* Navigate to **Settings** > **Apps** > **Special app access**.
* Select **Install unknown apps**.
* Choose the browser or file manager you will use to download the APK and toggle **Allow from this source** to on.

### 2. Download and Install
* Download the APK file to your device.
* Open your file manager and locate the downloaded file.
* Tap the file and select **Install**.

### 3. Initial Setup
* Launch the **Prayer Times Widget** app from your app drawer.
* Enter your city and country in the input field.
* Tap **Update Location** to save your preferences.

---

## How to Use the Widget

After configuring your location in the main app, you must manually add the widget to your home screen:

1. Long-press on an empty space on your home screen.
2. Select **Widgets** (or **Classic widgets** depending on your device software).
3. Locate **Prayer Times Widget** in the list.
4. Drag the 2x2 widget to your preferred screen location.
5. The widget will automatically begin calculating and displaying the time remaining until the next prayer based on your saved settings.

---

## Technical Details

* **Minimum API Level**: Android 8.0 (Oreo) or higher is required for background updates.
* **Data Provider**: Prayer times are calculated via the Aladhan API.
* **Update Frequency**: The widget uses internal alarms to refresh timings and maintain countdown accuracy without excessive battery drain.
