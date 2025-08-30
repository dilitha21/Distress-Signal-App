package com.example.distresssignalapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONArray;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SupabaseHelper {
    private static final String BASE_URL = "https://zyygedxhwpjtanigtmvr.supabase.co/rest/v1/emergency_contacts";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp5eWdlZHhod3BqdGFuaWd0bXZyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTYyNjQzOTcsImV4cCI6MjA3MTg0MDM5N30.FmuD3wAXXtpC19IFtAX11FRd6pOKbhbz7e2XvpOqYLI";

    private final OkHttpClient client;
    private final Context context;

    public interface ContactsCallback {
        void onSuccess(JSONArray contacts);
        void onFailure(Exception e);
    }

    public SupabaseHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public void fetchEmergencyContacts(String userId, ContactsCallback callback) {
        if (!isNetworkAvailable()) {
            callback.onFailure(new IOException("No internet connection"));
            return;
        }

        String url = BASE_URL + "?user_id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray contacts = new JSONArray(responseData);
                        callback.onSuccess(contacts);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new IOException("Response not successful: " + response.code()));
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}