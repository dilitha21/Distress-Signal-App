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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "DistressApp";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String SUPABASE_URL = "https://zyygerdxhwpjtanigtmvr.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp5eWdlZHhod3BqdGFuaWd0bXZyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTYyNjQzOTcsImV4cCI6MjA3MTg0MDM5N30.FmuD3wAXXtpC19IFtAX11FRd6pOKbhbz7e2XvpOqYLI";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    private LocationManager locationManager;
    private Location currentLocation;
    private TextView statusTextView;
    private Button sendDistressButton;
    private OkHttpClient httpClient;

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
            initializeHttpClient();
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

    private void initializeHttpClient() {
        try {
            httpClient = new OkHttpClient();
            Log.d(TAG, "HTTP client initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing HTTP client: " + e.getMessage(), e);
        }
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
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.d(TAG, "Permission denied: " + permissions[i]);
                }
            }

            if (!allGranted) {
                updateStatus("Please grant all permissions for the app to work");
                Toast.makeText(this, "All permissions are required for the app to work properly", Toast.LENGTH_LONG).show();
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
                    // Request location updates
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);
                        Log.d(TAG, "GPS location updates requested");
                    }

                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, this);
                        Log.d(TAG, "Network location updates requested");
                    }

                    // Try to get last known location
                    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLocation == null) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }

                    if (lastLocation != null) {
                        currentLocation = lastLocation;
                        updateStatus("Location ready. Press volume up 3 times or use button to send distress signal.");
                        Log.d(TAG, "Got last known location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                    } else {
                        updateStatus("Getting location... Press volume up 3 times or use button when ready.");
                        Log.d(TAG, "No last known location available");
                    }
                }
            } else {
                updateStatus("Location permission required");
                Log.e(TAG, "Location permission not granted");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception in location manager: " + e.getMessage(), e);
            updateStatus("Location permission error");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing location manager: " + e.getMessage(), e);
            updateStatus("Error setting up location");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            long currentTime = System.currentTimeMillis();

            // Reset count if timeout exceeded
            if (currentTime - lastVolumeUpPress > VOLUME_PRESS_TIMEOUT) {
                volumeUpPressCount = 0;
            }

            volumeUpPressCount++;
            lastVolumeUpPress = currentTime;

            Log.d(TAG, "Volume up pressed " + volumeUpPressCount + " times");

            if (volumeUpPressCount >= 3) {
                volumeUpPressCount = 0;
                sendDistressSignal();
                return true; // Consume the event
            }

            updateStatus("Volume up pressed " + volumeUpPressCount + "/3 times");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void sendDistressSignal() {
        Log.d(TAG, "Sending distress signal");
        updateStatus("Sending distress signal...");

        // Send even if location is not available yet
        getEmergencyContacts();
    }

    private void getEmergencyContacts() {
        String url = SUPABASE_URL + "/rest/v1/emergency_contacts?user_id=eq." + USER_ID;
        Log.d(TAG, "Fetching emergency contacts from: " + url);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Failed to get emergency contacts", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus("Failed to get emergency contacts. Check internet connection.");
                            Toast.makeText(MainActivity.this, "Failed to send distress signal - No internet", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.d(TAG, "Response code: " + response.code());

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Response body: " + responseBody);

                        try {
                            JSONArray contacts = new JSONArray(responseBody);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sendSMSToContacts(contacts);
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing contacts", e);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateStatus("Error parsing contacts: " + e.getMessage());
                                }
                            });
                        }
                    } else {
                        Log.e(TAG, "Failed response code: " + response.code());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus("Failed to fetch contacts. Response code: " + response.code());
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error making request", e);
            updateStatus("Error making request: " + e.getMessage());
        }
    }

    private void sendSMSToContacts(JSONArray contacts) {
        if (contacts.length() == 0) {
            updateStatus("No emergency contacts found");
            Toast.makeText(this, "No emergency contacts configured", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No emergency contacts found");
            return;
        }

        String message = createDistressMessage();
        Log.d(TAG, "Distress message: " + message);

        try {
            SmsManager smsManager = SmsManager.getDefault();
            int sentCount = 0;

            for (int i = 0; i < contacts.length(); i++) {
                try {
                    JSONObject contact = contacts.getJSONObject(i);
                    String phoneNumber = contact.getString("contact_phone");
                    String contactName = contact.getString("contact_name");

                    Log.d(TAG, "Sending SMS to " + contactName + " at " + phoneNumber);
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                    sentCount++;

                } catch (Exception e) {
                    Log.e(TAG, "Error sending SMS to contact " + i, e);
                }
            }

            final int finalSentCount = sentCount;
            updateStatus("Distress signal sent to " + finalSentCount + " contact(s)");
            Toast.makeText(this, "Distress signal sent to " + finalSentCount + " contact(s)", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Distress signal sent to " + finalSentCount + " contacts");

        } catch (Exception e) {
            Log.e(TAG, "Error in sendSMSToContacts", e);
            updateStatus("Error sending SMS: " + e.getMessage());
        }
    }

    private String createDistressMessage() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String locationText = "Location not available";

        if (currentLocation != null) {
            locationText = String.format(Locale.getDefault(),
                    "Lat: %.6f, Lng: %.6f\nGoogle Maps: https://maps.google.com/?q=%.6f,%.6f",
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            Log.d(TAG, "Using current location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
        } else {
            Log.w(TAG, "No location available for distress message");
        }

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
        Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Location provider " + provider + " status changed: " + status);
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