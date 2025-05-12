plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ir.mthri.shekar"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.mthri.shekar"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // برای OkHttp (مدیریت درخواست‌های شبکه)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // برای Jsoup (تجزیه HTML)
    implementation("org.jsoup:jsoup:1.17.2")
    // برای Gson (کار با JSON)
    implementation("com.google.code.gson:gson:2.10.1")
    // برای Kotlin Coroutines (عملیات ناهمزمان)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

}