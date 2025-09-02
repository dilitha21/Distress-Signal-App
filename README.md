# Distress Signal App

An Android application that allows users to quickly send distress signals by pressing the volume up button 3 times. The app runs as a background service and includes location tracking capabilities.

## Features

- Send distress signals using volume button triggers
- Background service monitoring for emergency signals
- Real-time GPS location tracking
- SMS messaging capabilities
- Network status monitoring
- Foreground service notifications

## Requirements

- Android 8.0 (API level 26) or higher
- GPS enabled device
- Internet connection (optional)
- SMS capabilities (optional)

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Build and run the application

## Permissions

The app requires the following permissions:
- SMS sending
- Fine and coarse location access
- Internet access
- Network state access
- Foreground service
- System alert window
- Wake lock

## Usage

1. Launch the app
2. Grant required permissions
3. Press "Start Service" to begin monitoring
4. Press volume up button 3 times quickly to trigger distress signal
5. Use "Stop Service" to disable monitoring

## Dependencies

- AndroidX AppCompat
- Google Material Design Components
- OkHttp for network requests
- Google Play Services Location
- Gson for JSON parsing

## Build Configuration

- Minimum SDK: 26
- Target SDK: 27
- Compile SDK: 34

