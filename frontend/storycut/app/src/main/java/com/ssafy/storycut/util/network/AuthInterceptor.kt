package com.ssafy.storycut.util.network

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ssafy.storycut.MainActivity
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.data.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthInterceptor"

/**
 * 인증 토큰을 요청 헤더에 추가하고, 401 응답 시 토큰 갱신을 처리하는 인터셉터
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository, // 인증 리포지토리 주입
    private val context: Context // 컨텍스트 주입 추가
) : Interceptor {

    private val noAuthPaths = listOf(
        "/api/v1/spring/auth/login",
    )

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // 원본 요청 가져오기
        val originalRequest: Request = chain.request()

        // 현재 요청 URL 경로 가져오기
        val url = originalRequest.url.toString()

        // 인증이 필요없는 경로인지 확인
        val isAuthExempt = noAuthPaths.any { path -> url.contains(path) }

        // 인증 면제 경로면 원본 요청 그대로 진행, 아니면 토큰 추가
        val requestWithToken = if (isAuthExempt) {
            Log.d(TAG, "인증 면제 경로: $url - 토큰 추가하지 않음")
            originalRequest
        } else {
            addTokenToRequest(originalRequest)
        }

        // 수정된 요청으로 서버 응답 받기
        var response = chain.proceed(requestWithToken)

        // 응답 본문 체크 - 토큰 만료 확인
        val responseBody = response.body
        if (responseBody != null) {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // 전체 본문 버퍼링
            val buffer = source.buffer.clone()
            val responseBodyString = buffer.readUtf8()

            try {
                val jsonObject = JSONObject(responseBodyString)
                // code 필드가 1002인지 확인 (토큰 만료)
                if (jsonObject.has("code") && jsonObject.getInt("code") == 1002) {
                    Log.d(TAG, "코드 1002 응답 수신. 액세스 토큰 만료. 토큰 갱신 시도...")

                    // 응답 바디 복제 (다시 읽을 수 있도록)
                    val responseBodyCopy = responseBodyString.toResponseBody(responseBody.contentType())

                    // 새 응답 객체 생성 (원본 응답 바디 소비 방지)
                    val originalResponse = response.newBuilder()
                        .body(responseBodyCopy)
                        .build()

                    // 토큰 갱신 시도
                    val newToken = runBlocking { refreshToken() }

                    // 토큰 갱신 성공 시 원본 요청에 새 토큰 추가하여 재시도
                    if (newToken != null) {
                        Log.d(TAG, "토큰 갱신 성공. 원본 요청 재시도")

                        // 기존 응답 바디 닫기
                        response.close()

                        // 새 토큰으로 원본 요청 재시도
                        val newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()

                        return chain.proceed(newRequest)
                    } else {
                        Log.e(TAG, "토큰 갱신 실패. 인증 오류 응답 반환")

                        // 토큰 갱신 실패 시 저장된 토큰 삭제
                        runBlocking {
                            try {
                                tokenManager.clearTokens()
                                Log.d(TAG, "토큰 갱신 실패로 로컬 토큰 삭제 완료")

                                // 메인 스레드에서 로그인 화면으로 이동
                                navigateToLogin()
                            } catch (e: Exception) {
                                Log.e(TAG, "토큰 삭제 중 오류 발생", e)
                            }
                        }

                        // 원본 응답 반환
                        return originalResponse
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "응답 본문 처리 중 오류 발생", e)
            }

            // 응답 바디 복제 (다시 읽을 수 있도록)
            val newResponseBody = responseBodyString.toResponseBody(responseBody.contentType())

            // 수정된 응답 반환
            return response.newBuilder()
                .body(newResponseBody)
                .build()
        }

        return response
    }

    /**
     * 요청에 액세스 토큰을 추가하는 함수
     */
    private fun addTokenToRequest(request: Request): Request {
        val accessToken = runBlocking { tokenManager.accessToken.firstOrNull() }

        return if (!accessToken.isNullOrEmpty()) {
            Log.d(TAG, "요청 헤더에 토큰 추가")
            request.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            Log.d(TAG, "토큰이 없음, 기본 요청 반환")
            request
        }
    }

    /**
     * 토큰 갱신 시도 함수
     * @return 성공 시 새 액세스 토큰, 실패 시 null
     */
    private suspend fun refreshToken(): String? {
        try {
            // 리포지토리를 통해 토큰 갱신
            val tokens = authRepository.refreshAccessToken()

            return tokens?.first // 새 액세스 토큰
        } catch (e: Exception) {
            Log.e(TAG, "토큰 갱신 중 예외 발생", e)
            return null
        }
    }

    private fun navigateToLogin() {
        Handler(Looper.getMainLooper()).post {
            // MainActivity를 다시 시작하면서 로그인 상태로 이동하도록 FLAG 추가
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // 로그인 화면으로 이동해야 함을 알리는 추가 플래그
                putExtra("NAVIGATE_TO_LOGIN", true)
            }
            context.startActivity(intent)
        }
    }
}