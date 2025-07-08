import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("keystore.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val dropboxKey: String = localProperties.getProperty("DROPBOX_KEY") ?: ""


plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.github.bytesculptor07.quillo"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.github.bytesculptor07.quillo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DROPBOX_KEY", "\"$dropboxKey\"")
        manifestPlaceholders["dropboxKey"] = "db-$dropboxKey"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.otaliastudios:zoomlayout:1.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("com.google.mlkit:digital-ink-recognition:18.1.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.github.skydoves:colorpickerview:2.3.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.dropbox.core:dropbox-core-sdk:7.0.0")
    implementation("com.dropbox.core:dropbox-android-sdk:7.0.0")
    implementation("com.websitebeaver:documentscanner:1.3.5")
    api("com.squareup.okhttp3:okhttp:4.12.0")
}