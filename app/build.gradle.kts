plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.distresssignalapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.distresssignalapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Core Android libraries
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")

    // HTTP client for Supabase
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Location services (optional - using built-in LocationManager)
    implementation("com.google.android.gms:play-services-location:18.0.0")

    // JSON parsing (built-in, but adding Gson as backup)
    implementation("com.google.code.gson:gson:2.8.9")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}