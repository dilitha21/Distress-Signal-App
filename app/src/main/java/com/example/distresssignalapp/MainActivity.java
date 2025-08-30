package com.example.distresssignalapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "DistressApp";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    private LocationManager locationManager;
    private Location currentLocation;
    private TextView statusTextView;
    private Button sendDistressButton;
    private SupabaseHelper supabaseHelper;

    private int volumeUpPressCount = 0;
    private long lastVolumeUpPress = 0;
    private static final long VOLUME_PRESS_TIMEOUT = 2000; // 2 seconds timeout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            Log.d(TAG, "Activity created successfully");

            initializeViews();
            initializeSupabaseHelper();
            checkPermissions();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    private void initializeViews() {
        try {
            statusTextView = findViewById(R.id.statusTextView);
            sendDistressButton = findViewById(R.id.sendDistressButton);

            sendDistressButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendDistressSignal();
                }
            });

            updateStatus("App initialized. Requesting permissions...");
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void initializeSupabaseHelper() {
        supabaseHelper = new SupabaseHelper(this);
        Log.d(TAG, "Supabase helper initialized");
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                Log.d(TAG, "Permission not granted: " + permission);
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        } else {
            Log.d(TAG, "All permissions already granted");
            initializeLocationManager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                updateStatus("Please grant all permissions for the app to work");
                Toast.makeText(this, "All permissions are required", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "All permissions granted");
                initializeLocationManager();
            }
        }
    }

    private void initializeLocationManager() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                if (locationManager != null) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);
                    }

                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, this);
                    }

                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLocation == null) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }

                    if (lastLocation != null) {
                        currentLocation = lastLocation;
                        updateStatus("Location ready. Press volume up 3 times or use button.");
                    } else {
                        updateStatus("Getting location... Please wait.");
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage(), e);
            updateStatus("Location permission error");
        }
    }

    private void getEmergencyContacts() {
        Log.d(TAG, "Fetching emergency contacts");
        updateStatus("Fetching emergency contacts...");

        supabaseHelper.fetchEmergencyContacts(USER_ID, new SupabaseHelper.ContactsCallback() {
            @Override
            public void onSuccess(JSONArray contacts) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sendSMSToContacts(contacts);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to get emergency contacts", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus("Failed to get emergency contacts: " + e.getMessage());
                        Toast.makeText(MainActivity.this,
                                "Failed to send distress signal - " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastVolumeUpPress > VOLUME_PRESS_TIMEOUT) {
                volumeUpPressCount = 0;
            }

            volumeUpPressCount++;
            lastVolumeUpPress = currentTime;

            if (volumeUpPressCount >= 3) {
                volumeUpPressCount = 0;
                sendDistressSignal();
                return true;
            }

            updateStatus("Volume up pressed " + volumeUpPressCount + "/3 times");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void sendDistressSignal() {
        Log.d(TAG, "Sending distress signal");
        updateStatus("Sending distress signal...");
        getEmergencyContacts();
    }

    private void sendSMSToContacts(JSONArray contacts) {
        if (contacts.length() == 0) {
            updateStatus("No emergency contacts found");
            Toast.makeText(this, "No emergency contacts configured", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = createDistressMessage();
        try {
            SmsManager smsManager = SmsManager.getDefault();
            int sentCount = 0;

            for (int i = 0; i < contacts.length(); i++) {
                try {
                    JSONObject contact = contacts.getJSONObject(i);
                    String phoneNumber = contact.getString("contact_phone");
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                    sentCount++;
                } catch (Exception e) {
                    Log.e(TAG, "Error sending SMS to contact " + i, e);
                }
            }

            updateStatus("Distress signal sent to " + sentCount + " contact(s)");
            Toast.makeText(this, "Distress signal sent to " + sentCount + " contact(s)", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS", e);
            updateStatus("Error sending SMS: " + e.getMessage());
        }
    }

    private String createDistressMessage() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String locationText = currentLocation != null ?
                String.format(Locale.getDefault(),
                        "Lat: %.6f, Lng: %.6f\nGoogle Maps: https://maps.google.com/?q=%.6f,%.6f",
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        currentLocation.getLatitude(), currentLocation.getLongitude())
                : "Location not available";

        return "🚨 EMERGENCY DISTRESS SIGNAL 🚨\n\n" +
                "This is an automated distress message from Dilitha.\n\n" +
                "Time: " + timestamp + "\n" +
                "Location: " + locationText + "\n\n" +
                "Please check on my safety immediately!";
    }

    private void updateStatus(String status) {
        if (statusTextView != null) {
            statusTextView.setText(status);
            Log.d(TAG, "Status updated: " + status);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLocation = location;
        updateStatus("Location updated. Ready to send distress signal.");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Required for interface but not used
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}