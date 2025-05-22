package com.ssafy.storycut.ui.shorts

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpResponseException
import com.ssafy.storycut.data.api.model.credential.GooglePermissionResponse
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI 상태를 나타내는 sealed class
sealed class ShortsUiState {
    object Loading : ShortsUiState()
    object Unauthenticated : ShortsUiState()
    object Authenticated : ShortsUiState()
}

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _youtubeAuthUrl = MutableLiveData<GooglePermissionResponse?>()
    val youtubeAuthUrl: LiveData<GooglePermissionResponse?> = _youtubeAuthUrl

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _accessToken = MutableLiveData<String?>()
    val accessToken: MutableLiveData<String?> = _accessToken

    // UI 상태 관리
    private val _uiState = MutableLiveData<ShortsUiState>(ShortsUiState.Loading)
    val uiState: LiveData<ShortsUiState> = _uiState

    init {
        // 초기화 시 토큰 확인
        loadAccessToken()
    }

    fun getYouTubeAuthUrl() {
        viewModelScope.launch {
            try {
                val response = repository.getYouTubeAuthUrl()
                _youtubeAuthUrl.value = response
            } catch (e: Exception) {
                _error.value = "인증 URL을 가져오는 데 실패했습니다: ${e.message}"
            }
        }
    }

    fun loadAccessToken() {
        viewModelScope.launch {
            try {
                _uiState.value = ShortsUiState.Loading
                val token = tokenManager.googleAccessToken.first()

                if (token.isNullOrEmpty()) {
                    _accessToken.value = ""
                    _uiState.value = ShortsUiState.Unauthenticated
                } else {
                    _accessToken.value = token
                    _uiState.value = ShortsUiState.Authenticated
                }
            } catch (e: Exception) {
                _error.value = "토큰을 불러오는 데 실패했습니다: ${e.message}"
                _uiState.value = ShortsUiState.Unauthenticated
            }
        }
    }

    // ShortsViewModel의 uploadToYouTube 함수
    fun uploadToYouTube(videoUri: Uri, title: String, description: String, tags: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                var token = tokenManager.googleAccessToken.first() ?: ""
                Log.d("ShortsViewModel", "토큰 가져오기 성공: ${token}")

                try {
                    // 첫 번째 시도
                    repository.uploadVideoToYouTube(token, videoUri, title, description, tags)
                    _error.value = "유튜브 업로드가 성공적으로 시작되었습니다!"
                } catch (e: Exception) {
                    // 401 에러(Unauthorized)인 경우 토큰 갱신 후 재시도
                    if (isUnauthorizedError(e)) {
                        Log.d("ShortsViewModel", "401 에러 발생, 토큰 갱신 시도")
                        try {
                            Log.d("AuthViewModel", "토큰 갱신 직전")
                            // 토큰 갱신 - 이제 바로 토큰 문자열을 반환받음
                            token = repository.refreshGoogleToken()
                            Log.d("AuthViewModel", "토큰 고고 ${token}")
                            // 갱신된 토큰으로 다시 시도
                            repository.uploadVideoToYouTube(token, videoUri, title, description, tags)
                            _error.value = "유튜브 업로드가 성공적으로 시작되었습니다!"
                        } catch (refreshError: Exception) {
                            _error.value = "토큰 갱신 실패: ${refreshError.message}"
                            Log.e("ShortsViewModel", "토큰 갱신 실패", refreshError)
                        }
                    } else {
                        _error.value = "업로드 실패: ${e.message}"
                        Log.e("ShortsViewModel", "업로드 실패", e)
                    }
                }
            } catch (e: Exception) {
                _error.value = "업로드 준비 실패: ${e.message}"
                Log.e("ShortsViewModel", "업로드 준비 실패", e)
            }
        }
    }

    // 에러 메시지 초기화
    fun clearError() {
        _error.value = null
    }

    // 401 에러인지 확인하는 헬퍼 함수
    private fun isUnauthorizedError(e: Exception): Boolean {
        // Google API에서 발생하는 401 에러 체크
        return when (e) {
            is GoogleJsonResponseException -> e.statusCode == 401
            is HttpResponseException -> e.statusCode == 401
            else -> e.message?.contains("401") == true ||
                    e.message?.contains("Unauthorized") == true ||
                    e.message?.contains("invalid_token") == true
        }
    }
}