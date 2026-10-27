plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.feedcleaner.linkedin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.feedcleaner.linkedin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
