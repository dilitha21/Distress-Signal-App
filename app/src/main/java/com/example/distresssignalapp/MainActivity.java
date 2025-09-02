package com.example.distresssignalapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button startServiceBtn, stopServiceBtn;
    private TextView statusText, locationText;
    private LocationManager locationManager;

    private int volumeUpPressCount = 0;
    private long lastVolumeUpTime = 0;
    private static final long VOLUME_PRESS_TIMEOUT = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        requestPermissions();
        setupLocationManager();
        setupButtons();
    }

    private void initViews() {
        startServiceBtn = findViewById(R.id.startServiceBtn);
        stopServiceBtn = findViewById(R.id.stopServiceBtn);
        statusText = findViewById(R.id.statusText);
        locationText = findViewById(R.id.locationText);
    }

    private void setupButtons() {
        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasAllPermissions()) {
                    startService(new Intent(MainActivity.this, DistressService.class));
                    statusText.setText("Service Status: Running");
                    Toast.makeText(MainActivity.this, "Distress service started", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please grant all permissions", Toast.LENGTH_SHORT).show();
                    requestPermissions();
                }
            }
        });

        stopServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, DistressService.class));
                statusText.setText("Service Status: Stopped");
                Toast.makeText(MainActivity.this, "Distress service stopped", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupLocationManager() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (hasAllPermissions()) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            handleVolumeUpPress();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void handleVolumeUpPress() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastVolumeUpTime > VOLUME_PRESS_TIMEOUT) {
            volumeUpPressCount = 1;
        } else {
            volumeUpPressCount++;
        }

        lastVolumeUpTime = currentTime;

        if (volumeUpPressCount >= 3) {
            triggerDistressSignal();
            volumeUpPressCount = 0;
        }
    }

    private void triggerDistressSignal() {
        if (hasAllPermissions()) {
            Intent intent = new Intent(this, DistressService.class);
            intent.putExtra("trigger_distress", true);
            startService(intent);
            Toast.makeText(this, "DISTRESS SIGNAL SENT!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        String locationStr = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
        locationText.setText("Location: " + locationStr);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}