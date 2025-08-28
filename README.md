
# Distress Signal App

An **Android app in Java** that sends an emergency SMS to predefined contacts when you either:

* Press the **volume up button 3 times** within 2 seconds.
* Tap the **distress button** in the app UI.

The app integrates with **Supabase** to fetch emergency contacts and then uses the device’s **SmsManager** to send messages.

---

## Features

* Detect **3 volume up presses** as a distress trigger.
* **Manual distress button** in the app UI.
* Sends SMS directly using **SmsManager** (no external SMS API needed).
* Fetches emergency contacts dynamically from **Supabase**.
* Includes location (latitude, longitude, and Google Maps link) in messages.

---

## Project Structure

```
DistressSignalApp/
├── app/src/main/java/com/example/distresssignalapp/
│   └── MainActivity.java        # Core logic (UI, SMS, location, Supabase API)
│
├── app/src/main/res/layout/
│   └── activity_main.xml        # Contains distress button + status text
│
├── app/src/main/AndroidManifest.xml
```

---

## Database (Supabase PostgreSQL)

### Users table

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Sample user
INSERT INTO users (id, phone_number)
VALUES ('11111111-1111-1111-1111-111111111111', '+94770000000');
```

### Emergency Contacts table

```sql
CREATE TABLE emergency_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    contact_name TEXT NOT NULL,
    contact_phone TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Sample contact
INSERT INTO emergency_contacts (user_id, contact_name, contact_phone)
VALUES ('11111111-1111-1111-1111-111111111111', 'Dad', '+94771234567');
```

---

## Android Setup

### 1. Permissions in `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```

### 2. Dependencies (`build.gradle`)

Add **OkHttp** for Supabase REST API calls:

```gradle
implementation 'com.squareup.okhttp3:okhttp:4.9.3'
```

---

## Core File: `MainActivity.java`

Handles:

* Permission requests.
* Volume button press detection.
* Location updates.
* Supabase REST calls (via OkHttp).
* SMS sending with **SmsManager**.

---

## Layout: `activity_main.xml`

Example:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="20dp">

    <TextView
        android:id="@+id/statusTextView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Initializing..."
        android:textSize="18sp"
        android:padding="10dp"/>

    <Button
        android:id="@+id/sendDistressButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Send Distress Signal"/>
</LinearLayout>
```

---

## Running the App

1. Clone the project into Android Studio.
2. Add your **Supabase URL** and **Anon Key** inside `MainActivity.java`.
3. Insert your **user and emergency contacts** into Supabase.
4. Run the app on a real device with a SIM card.
5. Test:

   * Press **volume up 3 times quickly**.
   * Or tap the **distress button**.
6. Emergency contacts will receive an SMS with your **location and timestamp**.

---

## Notes

* SMS requires SIM card and credit.
* Works only on physical devices, not emulators.
* At least one contact must exist in `emergency_contacts`.
* Location accuracy depends on GPS and network availability.


