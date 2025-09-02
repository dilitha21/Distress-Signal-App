package com.example.distresssignalapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DistressService extends Service implements LocationListener {

    private static final String CHANNEL_ID = "DistressServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    // HARDCODED EMERGENCY CONTACT
    private static final String EMERGENCY_PHONE = "+94718303512";

    private LocationManager locationManager;
    private Location lastKnownLocation;
    private int volumeUpCount = 0;
    private long lastVolumeTime = 0;
    private static final long VOLUME_TIMEOUT = 2000; // 2 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupLocationManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("trigger_distress", false)) {
            sendDistressSignal();
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Distress Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Emergency distress service running");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Emergency Service Active")
                .setContentText("Press Volume Up 3x for distress signal")
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void setupLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                // Get last known location
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                // Request location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);

            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendDistressSignal() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String message = createDistressMessage();

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(EMERGENCY_PHONE, null, message, null, null);

            Toast.makeText(this, "Emergency SMS sent!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String createDistressMessage() {
        StringBuilder message = new StringBuilder();
        message.append(" EMERGENCY DISTRESS SIGNAL \n");

        // Add timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        message.append("Time: ").append(sdf.format(new Date())).append("\n");

        // Add location if available
        if (lastKnownLocation != null) {
            message.append("Location: ");
            message.append("Lat: ").append(lastKnownLocation.getLatitude()).append(", ");
            message.append("Lng: ").append(lastKnownLocation.getLongitude()).append("\n");

            // Add Google Maps link
            message.append("Map: https://maps.google.com/maps?q=");
            message.append(lastKnownLocation.getLatitude());
            message.append(",").append(lastKnownLocation.getLongitude()).append("\n");
        } else {
            message.append("Location: Unable to determine\n");
        }

        message.append("Please send help immediately!");

        return message.toString();
    }

    // Volume button detection in service
    public boolean handleVolumeKey() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastVolumeTime > VOLUME_TIMEOUT) {
            volumeUpCount = 1;
        } else {
            volumeUpCount++;
        }

        lastVolumeTime = currentTime;

        if (volumeUpCount >= 3) {
            sendDistressSignal();
            volumeUpCount = 0;
            return true;
        }

        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}