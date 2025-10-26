
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
            // Run location fetch + SMS send off the main thread to avoid blocking UI
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendDistressSignal();
                }
            }).start();
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
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
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

                // Request ongoing location updates for background tracking
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

        // Ensure we have a recent location before creating message
        ensureLocationAvailable(5000); // wait up to 5 seconds for a quick location fix

        String message = createDistressMessage();

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(EMERGENCY_PHONE, null, message, null, null);

            // Toast must run on main thread
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "Emergency SMS sent!", Toast.LENGTH_LONG).show()
            );

        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }
    }

    /**
     * Ensure lastKnownLocation is populated: try lastKnownLocation first, then request quick updates
     * and wait up to timeoutMillis for a callback.
     */
    private void ensureLocationAvailable(long timeoutMillis) {
        if (lastKnownLocation != null) return;

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            // Try getting last known locations again
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (gps != null && net != null) {
                lastKnownLocation = gps.getTime() >= net.getTime() ? gps : net;
            } else if (gps != null) {
                lastKnownLocation = gps;
            } else if (net != null) {
                lastKnownLocation = net;
            }

            if (lastKnownLocation != null) return;

            // If still null, request quick one-time updates and wait up to timeoutMillis
            final CountDownLatch latch = new CountDownLatch(1);

            LocationListener tempListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        lastKnownLocation = location;
                        latch.countDown();
                    }
                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };

            // Request rapid updates; use main looper so callbacks occur
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, tempListener, Looper.getMainLooper());
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, tempListener, Looper.getMainLooper());

            try {
                latch.await(Math.max(500, timeoutMillis), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                try {
                    locationManager.removeUpdates(tempListener);
                } catch (SecurityException ignored) {}
            }

        } catch (SecurityException e) {
            e.printStackTrace();
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
            // ensure sending does not block main thread
            new Thread(this::sendDistressSignal).start();
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
