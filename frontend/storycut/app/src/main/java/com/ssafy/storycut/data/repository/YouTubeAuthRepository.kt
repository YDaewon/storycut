package com.ssafy.storycut.data.repository


import com.ssafy.storycut.data.api.model.BaseResponse
import com.ssafy.storycut.data.api.model.credential.GooglePermissionResponse
import com.ssafy.storycut.data.api.service.AuthApiService
import com.ssafy.storycut.data.local.datastore.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class YouTubeAuthRepository @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager
) {
    suspend fun getYouTubeAuthUrl(): GooglePermissionResponse {
        return withContext(Dispatchers.IO) {
            // 저장된 액세스 토큰 가져오기
            val token = tokenManager.accessToken.first()
            if (token.isNullOrEmpty()) {
                throw Exception("인증 토큰이 없습니다. 로그인이 필요합니다.")
            }

            val authHeader = "Bearer $token"
            val response = authApi.getYouTubeAuthUrl(authHeader)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.code.toLong() == 200L) {  // Int와 Long 비교 수정
                    // null 체크 후 null이 아닌 경우에만 반환
                    body.result ?: throw Exception("응답 데이터가 비어있습니다.")
                } else {
                    throw Exception(body?.message ?: "응답 데이터가 올바르지 않습니다.")
                }
            } else {
                throw Exception("API 호출 실패: ${response.code()} - ${response.message()}")
            }
        }
    }
}