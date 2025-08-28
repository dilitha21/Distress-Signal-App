package com.example.distresssignalapp;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SupabaseHelper {

    private static final String BASE_URL = "https://zyygedxhwpjtanigtmvr.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp5eWdlZHhod3BqdGFuaWd0bXZyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTYyNjQzOTcsImV4cCI6MjA3MTg0MDM5N30.FmuD3wAXXtpC19IFtAX11FRd6pOKbhbz7e2XvpOqYLI";

    private OkHttpClient client = new OkHttpClient();

    public interface ContactsCallback {
        void onSuccess(List<String> phoneNumbers);
        void onFailure(Exception e);
    }

    public void fetchEmergencyContacts(String userId, ContactsCallback callback) {
        String url = BASE_URL + "?user_id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        List<String> contacts = new ArrayList<>();
                        String json = response.body().string();
                        JSONArray jsonArray = new JSONArray(json);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            contacts.add(obj.getString("phone_number"));
                        }
                        callback.onSuccess(contacts);
                    } else {
                        callback.onFailure(new Exception("Failed to fetch contacts"));
                    }
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }
}
