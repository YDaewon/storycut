package com.ssafy.storycut.util.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.ssafy.storycut.MainActivity
import com.ssafy.storycut.R
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.data.local.datastore.FCMTokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    private val TAG = "FCM_Service"
    private val CHANNEL_ID = "video_processing"
    private val CHANNEL_NAME = "영상 처리 알림"

    /**
     * 새 토큰이 생성될 때 호출됨
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "새로운 토큰: $token")

        // 새 토큰을 저장
        fcmTokenManager.saveToken(token)
    }

    /**
     * FCM 메시지 수신 시 호출됨
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM 메시지 수신: ${remoteMessage.from}")

        // 데이터 메시지 처리
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "데이터 메시지: ${remoteMessage.data}")

            val isSuccess = remoteMessage.data["isSuccess"]?.toBoolean() ?: false
            val code = remoteMessage.data["code"]?.toInt() ?: 0
            val message = remoteMessage.data["message"] ?: ""
            val resultJson = remoteMessage.data["result"]

            // 영상 처리 완료된 경우
            if (isSuccess && code == 200 && !resultJson.isNullOrEmpty()) {
                try {
                    // JSON 파싱
                    val videoDto = parseVideoDto(resultJson)

                    // 알림 표시 - 제목에 비디오 타이틀 추가
                    val notificationTitle = remoteMessage.notification?.title ?: "영상 편집 완료"
                    val notificationBody = "영상 '${videoDto.videoTitle}'의 편집이 완료되었습니다."

                    sendNotification(notificationTitle, notificationBody, videoDto)
                } catch (e: Exception) {
                    Log.e(TAG, "JSON 파싱 오류", e)

                    // 파싱 오류 시에도 기본 알림 표시
                    val title = remoteMessage.notification?.title ?: "영상 편집 완료"
                    val body = remoteMessage.notification?.body ?: "영상 처리가 완료되었습니다."
                    sendDefaultNotification(title, body)
                }
            } else {
                // 일반 알림 표시
                val title = remoteMessage.notification?.title ?: "StoryCut"
                val body = remoteMessage.notification?.body ?: ""
                sendDefaultNotification(title, body)
            }
        } else if (remoteMessage.notification != null) {
            // 알림 메시지만 있는 경우
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body ?: ""
            sendDefaultNotification(title, body)
        }
    }

    /**
     * JSON 문자열을 VideoDto 객체로 파싱
     */
    private fun parseVideoDto(jsonString: String): VideoDto {
        return Gson().fromJson(jsonString, VideoDto::class.java)
    }

    /**
     * 비디오 정보가 포함된 알림 표시
     */
    private fun sendNotification(title: String, messageBody: String, videoDto: VideoDto) {
        // 클릭 시 실행할 인텐트 설정
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // 비디오 정보 추가
            putExtra("videoId", videoDto.videoId.toString())
            putExtra("fromFcm", true)  // FCM에서 시작됨을 표시
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, videoDto.videoId.toInt(), intent, pendingIntentFlags
        )

        // 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "영상 처리 완료 알림"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 비디오 ID를 알림 ID로 사용하여 각 영상마다 별도의 알림 표시
        notificationManager.notify(videoDto.videoId.toInt(), notificationBuilder.build())
    }

    /**
     * 기본 알림 표시 (비디오 정보 없음)
     */
    private fun sendDefaultNotification(title: String?, messageBody: String) {
        // 클릭 시 실행할 인텐트 설정
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("fromFcm", true)  // FCM에서 시작됨을 표시
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, pendingIntentFlags
        )

        // 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: "StoryCut")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }
}