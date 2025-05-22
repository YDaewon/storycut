package com.ssafy.storycut.data.api

import com.google.gson.GsonBuilder
import com.ssafy.storycut.BuildConfig
import com.ssafy.storycut.data.api.service.AuthApiService
import com.ssafy.storycut.data.api.service.EditService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = BuildConfig.BASE_URL
private const val AI_URL = BuildConfig.AI_URL

object RetrofitClient {
    // 로깅 인터셉터 설정 - 개발 빌드에서만 BODY 레벨, 릴리스에서는 NONE
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    // OkHttpClient 설정
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // 파일 업로드를 위한 긴 타임아웃을 가진 OkHttpClient 설정
    private val fileUploadOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(3, TimeUnit.MINUTES)     // 3분으로 증가
        .readTimeout(5, TimeUnit.MINUTES)        // 5분으로 증가
        .writeTimeout(10, TimeUnit.MINUTES)      // 10분으로 증가 (업로드에 가장 중요)
        .build()


    private val gson = GsonBuilder()
        .setLenient()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        .create()

    // Retrofit 인스턴스 생성 (공통)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(fileUploadOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // 두 번째 서버를 위한 Retrofit 인스턴스
    private val secondRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AI_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // AuthApiService 인스턴스
    val authService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val editService: EditService by lazy {
        secondRetrofit.create(EditService::class.java)
    }

}