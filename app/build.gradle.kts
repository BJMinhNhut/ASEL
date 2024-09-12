import com.android.build.api.dsl.Packaging
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.cs426.asel"
    compileSdk = 34
    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "com.cs426.asel"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "API_KEY1",
            "\"${localProperties.getProperty("apiKey1", "")}\""
        )
        buildConfigField(
            "String",
            "API_KEY2",
            "\"${localProperties.getProperty("apiKey2", "")}\""
        )
        buildConfigField(
            "String",
            "API_KEY3",
            "\"${localProperties.getProperty("apiKey3", "")}\""
        )
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
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.legacy.support.v4)
    implementation("com.google.android.gms:play-services-auth:20.4.1")
    implementation("com.google.api-client:google-api-client:1.33.0")
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.api-client:google-api-client-gson:1.33.0")
    implementation("com.google.apis:google-api-services-gmail:v1-rev110-1.25.0")
    implementation("com.google.http-client:google-http-client-jackson2:1.41.5")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation(libs.jsoup)
    implementation(libs.generativeai)
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation(libs.javax.mail)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}