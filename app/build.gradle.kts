plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.EcoPOS_V2S"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.EcoPOS_V2S"
        minSdk = 25
        targetSdk = 34
        versionCode = 11
        versionName = "1.11"

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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.15.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("com.sunmi:printerlibrary:1.0.18")
    implementation("org.json:json:20210307")
    implementation("com.google.zxing:core:3.4.0")
    implementation("com.journeyapps:zxing-android-embedded:4.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(files("libs/MyPayCommon_v0.3.3_release.aar"))
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.caverock:androidsvg-aar:1.4")

}
