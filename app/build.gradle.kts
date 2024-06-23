plugins {
    alias(libs.plugins.android.application)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.android.walksafe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.walksafe"
        minSdk = 24
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //JetBrains (Avoid DuplicateClasses) to manage Kotlin versions and ensure consistency across dependencies
    implementation (platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))

    //Firestore Firebase
    implementation("com.google.firebase:firebase-firestore:25.0.0")
    implementation ("com.google.gms:google-services:4.4.2")

    //Realtime Database
    implementation ("com.google.firebase:firebase-database:21.0.0")

    //Google Maps
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation("androidx.activity:activity:1.9.0")

    //Firebase
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")

    //Place SDK
    implementation("com.google.android.libraries.places:places:3.5.0")

}