package com.ssafy.storycut.di

import android.content.Context
import com.ssafy.storycut.BuildConfig
import com.ssafy.storycut.data.api.service.AuthApiService
import com.ssafy.storycut.data.api.service.ChatApiService
import com.ssafy.storycut.data.api.service.EditService
import com.ssafy.storycut.data.api.service.RoomApiService
import com.ssafy.storycut.data.api.service.VideoApiService
import com.ssafy.storycut.data.local.datastore.FCMTokenManager
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.data.repository.AuthRepository
import com.ssafy.storycut.data.repository.EditRepository
import com.ssafy.storycut.data.repository.GoogleAuthService
import com.ssafy.storycut.data.repository.S3Repository
import com.ssafy.storycut.util.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

// 한정자(Qualifier) 정의
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainClient  // 새로운 한정자 추가

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = BuildConfig.BASE_URL
    private const val AI_URL = BuildConfig.AI_URL

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideFCMTokenManager(@ApplicationContext context: Context): FCMTokenManager {
        return FCMTokenManager(context)
    }

    @Provides
    @Singleton
    fun provideGoogleAuthService(): GoogleAuthService {
        return GoogleAuthService()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        tokenManager: TokenManager,
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(tokenManager, context)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager,
        authRepository: AuthRepository,
        @ApplicationContext context: Context
    ): AuthInterceptor {
        return AuthInterceptor(tokenManager, authRepository, context)
    }

    @Provides
    @Singleton
    @PlainClient  // 한정자 추가
    fun providePlainOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 기본 Retrofit 인스턴스 (BASE_URL 사용)
    @Provides
    @Singleton
    @BaseRetrofit
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // AI URL을 위한 Retrofit 인스턴스
    @Provides
    @Singleton
    @AiRetrofit
    fun provideAiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AI_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(@BaseRetrofit retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVideoApiService(@BaseRetrofit retrofit: Retrofit): VideoApiService {
        return retrofit.create(VideoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRoomApiService(@BaseRetrofit retrofit: Retrofit): RoomApiService {
        return retrofit.create(RoomApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(@BaseRetrofit retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideS3Repository(@ApplicationContext context: Context): S3Repository {
        return S3Repository(context)
    }

    // EditService는 AI URL을 사용하는 Retrofit으로 생성
    @Provides
    @Singleton
    fun provideEditService(@AiRetrofit retrofit: Retrofit): EditService {
        return retrofit.create(EditService::class.java)
    }

    @Provides
    @Singleton
    fun provideEditRepository(
        editService: EditService,
        @ApplicationContext context: Context,
        tokenManager: TokenManager,
        fcmTokenManager: FCMTokenManager,
        @PlainClient plainOkHttpClient: OkHttpClient  // PlainClient 한정자 사용
    ): EditRepository {
        return EditRepository(editService, context, tokenManager, fcmTokenManager, plainOkHttpClient)
    }
}