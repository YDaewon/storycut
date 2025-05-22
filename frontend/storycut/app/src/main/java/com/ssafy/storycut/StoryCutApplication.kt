package com.ssafy.storycut

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.storycut.data.local.datastore.FCMTokenManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StoryCutApplication : Application() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    private val TAG = "StoryCutApplication"

    override fun onCreate() {
        super.onCreate()

        // 로그 필터 설정
        setupLogFilters()

        // FCM 채널 생성 및 토큰 등록
        setupFCM()
    }

    private fun setupLogFilters() {
        // 기존 코드 유지
        try {
            Log.i("LogFilter", "Log filter set up")
        } catch (e: Exception) {
            Log.e("LogFilter", "Failed to set up log filter", e)
        }
    }

    private fun setupFCM() {
        // Android 8.0 이상에서 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 비디오 처리 알림 채널 생성
            val videoChannelId = "video_processing"
            val videoChannelName = "영상 처리 알림"
            val videoChannel = NotificationChannel(
                videoChannelId,
                videoChannelName,
                NotificationManager.IMPORTANCE_HIGH  // 높은 중요도로 설정하여 알림음과 헤드업 알림 표시
            ).apply {
                description = "영상 처리 완료 알림"
                enableLights(true)       // LED 표시기 활성화
                enableVibration(true)    // 진동 활성화
                setShowBadge(true)       // 앱 아이콘에 배지 표시
            }

            // 채널 등록
            notificationManager.createNotificationChannel(videoChannel)

            Log.d(TAG, "알림 채널 생성 완료")
        }

        // FCM 토큰 가져오기
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }

            // 토큰 얻기 성공
            val token = task.result

            // 로컬에 토큰 저장
            fcmTokenManager.saveToken(token)

            // 로그로 토큰 출력
            Log.d(TAG, "FCM 토큰: $token")

        }
    }
}