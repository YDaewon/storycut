import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Kotlin 직렬화를 위한 플러그인 추가
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"

    // Room을 위한 KSP 플러그인
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")

}

// build.gradle.kts 파일 내에서
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

// local.properties에서 특정 값 가져오기
val sdkDir = localProperties.getProperty("sdk.dir") ?: ""
val BASE_URL = localProperties.getProperty("BASE_URL") ?: ""
val AI_URL = localProperties.getProperty("AI_URL") ?: ""
val WEB_CLIENT_ID = localProperties.getProperty("WEB_CLIENT_ID") ?: ""

android {
    namespace = "com.ssafy.storycut"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ssafy.storycut"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"$BASE_URL\"")
        buildConfigField("String", "AI_URL", "\"$AI_URL\"")
        buildConfigField("String", "WEB_CLIENT_ID", "\"$WEB_CLIENT_ID\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            pickFirsts.addAll(listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            ))
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.firebase.messaging.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 구글 로그인
    // Credential Manager (Google 로그인 대체)
    implementation("androidx.credentials:credentials:1.5.0")
    // optional - needed for credentials support from play services, for devices running
    // Android 13 and below.
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")

    implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Data Store
    implementation ("androidx.datastore:datastore-preferences:1.1.4")


    // 확인용
    implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.33.0")
    implementation("com.google.http-client:google-http-client-android:1.41.0")
    implementation("com.google.http-client:google-http-client-gson:1.41.0")

    // 서버 통신
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1") // Room 어노테이션 프로세서

    // navigation
    implementation("com.google.dagger:hilt-android:2.56.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-compiler:2.56.2")

    // coil
    implementation("io.coil-kt:coil-compose:2.4.0")
    
    // 영상재생
    implementation ("androidx.media3:media3-exoplayer:1.6.1")
    implementation ("androidx.media3:media3-ui:1.6.1")
    implementation ("androidx.media3:media3-common:1.6.1")

    //라이프 사이클
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    implementation("com.airbnb.android:lottie-compose:6.6.6")
    //FCM
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-messaging")

}