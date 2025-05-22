package com.ssafy.storycut

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.ui.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    // 딥링크 정보를 저장할 변수
    private val isDeepLinkState = mutableStateOf(false)

    // 알림 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "알림 권한 허용됨")
        } else {
            Log.d(TAG, "알림 권한 거부됨")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 알림 권한 요청 (Android 13 이상인 경우)
        requestNotificationPermission()

        // 초기 인텐트 처리
        handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isDeepLink = remember { isDeepLinkState }
                    val isFromFcm = remember { mutableStateOf(intent?.getBooleanExtra("fromFcm", false) ?: false) }

                    // 분리된 AppNavigation 컴포저블 사용
                    AppNavigation(
                        tokenManager = tokenManager,
                        isDeepLink = isDeepLink.value,
                        isFromFcm = isFromFcm.value
                    )
                }
            }
        }
    }

    // 알림 권한 요청 메서드
    private fun requestNotificationPermission() {
        // Android 13 (API 33) 이상인 경우에만 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // 이미 권한이 있는 경우
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "알림 권한 이미 허용됨")
                }

                // 권한 요청이 필요한 경우
                else -> {
                    Log.d(TAG, "알림 권한 요청 중...")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13 미만은 매니페스트에 권한만 선언하면 됨
            Log.d(TAG, "Android 13 미만 - 알림 권한 자동 허용됨")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // 새 인텐트 설정
        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent?) {
        // 딥링크 처리
        intent?.data?.let { uri ->
            val token = uri.getQueryParameter("token")
            if (!token.isNullOrEmpty()) {
                Log.d(TAG, "딥링크 URI: $uri, 토큰: $token")
                isDeepLinkState.value = true

                // 코루틴 스코프에서 토큰 저장
                lifecycleScope.launch {
                    tokenManager.saveGoogleAccessTokens(token)
                    Log.d(TAG, "구글 액세스 토큰 저장 완료")
                }
            }
        }

        // FCM 알림 처리 - Intent에 fromFcm 값을 전달하여 AppNavigation에서 처리할 수 있게 함
        if (intent?.getBooleanExtra("fromFcm", false) == true) {
            val videoId = intent.getStringExtra("videoId")
            Log.d(TAG, "FCM 알림에서 실행: videoId=$videoId")
            // 여기서는 isFromFcm 상태만 설정하고, 실제 화면 이동은 AppNavigation에서 처리
        }


        // FCM 알림 처리
        if (intent?.getBooleanExtra("fromFcm", false) == true) {
            val videoId = intent.getStringExtra("videoId")
            Log.d(TAG, "FCM 알림에서 실행: videoId=$videoId")
        }
    }
}