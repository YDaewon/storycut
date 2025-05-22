package com.ssafy.storycut.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.storycut.data.api.model.credential.UserInfo
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.data.repository.AuthRepository
import com.ssafy.storycut.data.repository.GoogleAuthService
import com.ssafy.storycut.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AuthRepository"

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthService: GoogleAuthService,
    private val userRepository: UserRepository, // Room DB 리포지토리 추가
    private val tokenManager: TokenManager // TokenManager 추가
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 토큰 상태
    private val _tokenState = MutableStateFlow<Pair<String, String>?>(null)
    val tokenState: StateFlow<Pair<String, String>?> = _tokenState.asStateFlow()
    
    // 사용자 정보 상태
    private val _userState = MutableStateFlow<UserInfo?>(null)
    val userState: StateFlow<UserInfo?> = _userState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // 네비게이션 이벤트 타입 정의
    sealed class NavigationEvent {
        object NavigateToLogin : NavigationEvent()
    }

    // 초기화 시 Room DB에서 사용자 정보와 토큰 유효성 확인
    init {
        // 매번 앱이 실행될 때마다 토큰 체크를 하도록 설정
        viewModelScope.launch {
            // 로컬 DB에서 사용자 정보 조회
            try {
                val user = userRepository.getCurrentUser().first()
                if (user != null) {
                    Log.d(TAG, "초기화: Room DB에서 사용자 정보 발견: $user")
                    _userState.value = user
                } else {
                    Log.d(TAG, "초기화: Room DB에 사용자 정보 없음")
                }
            } catch (e: Exception) {
                Log.e(TAG, "초기화: Room DB에서 사용자 정보 조회 중 오류", e)
            }
            
            // 토큰 유효성 확인 (로컬 사용자 정보와 도 반드시 실행)
            checkTokenValidity()
        }
    }
    
    // 로컬 DB에서 사용자 정보 조회 (초기화용)
    private fun loadUserFromDatabaseSilently() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser().first()
                if (user != null) {
                    Log.d(TAG, "초기화: Room DB에서 사용자 정보 발견: $user")
                    _userState.value = user
                    
                    // 토큰 유효성도 함께 확인
                    checkTokenValidity()
                } else {
                    Log.d(TAG, "초기화: Room DB에 사용자 정보 없음")
                    _userState.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "초기화: Room DB에서 사용자 정보 조회 중 오류", e)
                _userState.value = null
            }
        }
    }
    // 토큰 유효성 확인 - 스플래시에서 호출할 수 있도록 public으로 변경
    fun checkTokenValidity() {
        viewModelScope.launch {
            try {
                val accessToken = tokenManager.accessToken.first()

                if (!accessToken.isNullOrEmpty()) {
                    Log.d(TAG, "액세스 토큰 발견, 유효성 확인")

                    // 백그라운드에서 서버 정보 갱신 시도 (UI 영향 없이)
                    try {
                        val serverUserInfo = authRepository.getUserInfo()
                        if (serverUserInfo != null) {
                            // 서버에서 최신 정보를 가져왔다면 로컬 DB와 상태 업데이트
                            _userState.value = serverUserInfo
                            saveUserToDatabase(serverUserInfo)
                            Log.d(TAG, "토큰 유효함: 서버에서 사용자 정보 갱신 성공: $serverUserInfo")
                        }
                    } catch (e: Exception) {
                        // 토큰이 만료되었거나 서버 오류일 수 있음
                        Log.e(TAG, "액세스 토큰으로 서버 접근 실패, 리프레시 토큰 확인", e)

                        // 리프레시 토큰 확인
                        val refreshToken = tokenManager.refreshToken.first()
                        if (!refreshToken.isNullOrEmpty()) {
                            try {
                                val newTokens = authRepository.refreshAccessToken()
                                if (newTokens != null) {
                                    Log.d(TAG, "토큰 갱신 성공")

                                    // 토큰이 갱신되었으므로 서버에서 사용자 정보 다시 가져오기
                                    val userInfo = authRepository.getUserInfo()
                                    if (userInfo != null) {
                                        _userState.value = userInfo
                                        saveUserToDatabase(userInfo)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "리프레시 토큰으로 갱신 실패", e)
                                // userState는 로컬 DB의 정보로 유지
                            }
                        }
                    }
                } else {
                    // 액세스 토큰이 없는 경우
                    Log.d(TAG, "액세스 토큰 없음, 로그인 필요")
                    // 사용자 상태를 null로 설정하여 로그인 필요함을 표시
                    _userState.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "토큰 체크 과정에서 오류 발생", e)
                // 오류 발생 시 사용자 상태를 null로 설정
                _userState.value = null
            }
        }
    }
    
    // 마이페이지에서 사용할 Room DB 최신 사용자 정보 갱신 메서드
    fun refreshUserInfoFromRoom() {
        viewModelScope.launch {
            try {
                // 먼저 로컬 DB에서 사용자 정보 가져오기 시도
                val localUser = userRepository.getCurrentUser().first()
                
                if (localUser != null) {
                    // 로컬에서 사용자 정보를 찾았다면 상태 업데이트
                    _userState.value = localUser
                    Log.d(TAG, "Room DB에서 사용자 정보 불러오기 성공: $localUser")
                    
                    // 백그라운드에서 서버 정보 갱신 시도 (UI 차단 없이)
                    try {
                        val serverUserInfo = authRepository.getUserInfo()
                        if (serverUserInfo != null) {
                            // 서버에서 최신 정보를 가져왔다면 로컬 DB와 상태 업데이트
                            _userState.value = serverUserInfo
                            saveUserToDatabase(serverUserInfo)
                            Log.d(TAG, "서버에서 사용자 정보 갱신 성공: $serverUserInfo")
                        }
                    } catch (e: Exception) {
                        // 서버 통신 실패 시 로컬 데이터로만 유지
                        Log.e(TAG, "서버에서 사용자 정보 갱신 실패, 로컬 데이터 유지", e)
                    }
                } else {
                    // 로컬에 사용자 정보가 없다면 서버에서 가져오기 시도
                    Log.d(TAG, "Room DB에 사용자 정보 없음, 서버에서 가져오기 시도")
                    try {
                        val serverUserInfo = authRepository.getUserInfo()
                        if (serverUserInfo != null) {
                            // 서버에서 정보를 가져왔다면 로컬 DB와 상태 업데이트
                            _userState.value = serverUserInfo
                            saveUserToDatabase(serverUserInfo)
                            Log.d(TAG, "서버에서 사용자 정보 가져오기 성공: $serverUserInfo")
                        } else {
                            Log.e(TAG, "서버에서 사용자 정보 가져오기 실패")
                            // userState는 null 유지
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "서버에서 사용자 정보 가져오기 중 오류", e)
                        // userState는 null 유지
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 갱신 중 오류 발생", e)
            }
        }
    }
    
    // Room DB에서 사용자 정보 불러오기
    private fun loadUserFromDatabase() {
        viewModelScope.launch {
            try {
                // Room DB에서 가장 최신 사용자 정보를 가져오기 위해 first() 사용
                val user = userRepository.getCurrentUser().first()
                if (user != null) {
                    _userState.value = user
                    Log.d(TAG, "Room DB에서 사용자 정보 불러오기 성공: $user")
                } else {
                    Log.d(TAG, "Room DB에 저장된 사용자 정보 없음")
                    _userState.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Room DB에서 사용자 정보 불러오기 실패", e)
                _userState.value = null
            }
        }
    }

    // 구글 로그인 시작
    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                Log.d(TAG, "토큰 전송")
                googleAuthService.signInWithGoogle(context, credentialManager) { idToken ->
                    // 서버에 토큰 전송
                    sendTokenToServer(idToken)
                }
            } catch (e: Exception) {
                Log.e(TAG, "로그인 중 오류 발생", e)
                _uiState.value = AuthUiState.Error(e.message ?: "로그인 중 오류가 발생했습니다.")
            }
        }
    }

    // 서버에 토큰 전송
    private fun sendTokenToServer(idToken: String) {
        viewModelScope.launch {
            try {
                authRepository.sendTokenToServer(idToken) { result, tokens ->
                    if (tokens != null) {
                        _tokenState.value = tokens
                        
                        // 로그인 성공 시 사용자 정보 가져오기
                        getUserInfo()
                    } else {
                        _uiState.value = AuthUiState.Error(result)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "토큰 전송 중 오류 발생", e)
                _uiState.value = AuthUiState.Error(e.message ?: "서버 통신 중 오류가 발생했습니다.")
            }
        }
    }
    
    // 사용자 정보 가져오기
    private fun getUserInfo() {
        viewModelScope.launch {
            try {
                val userInfo = authRepository.getUserInfo()
                if (userInfo != null) {
                    // 사용자 정보 저장 및 성공 상태로 변경
                    _userState.value = userInfo
                    _uiState.value = AuthUiState.Success("로그인 성공")
                    Log.d(TAG, "사용자 정보 가져오기 성공: $userInfo")
                    
                    // Room DB에 사용자 정보 저장
                    saveUserToDatabase(userInfo)
                } else {
                    // 사용자 정보를 가져오는데 실패했지만, 로그인은 성공
                    _uiState.value = AuthUiState.Success("로그인 성공했지만 사용자 정보를 가져오는데 실패했습니다.")
                    Log.e(TAG, "사용자 정보 가져오기 실패")
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 가져오기 중 오류 발생", e)
                // 사용자 정보를 가져오는데 실패했지만, 로그인은 성공
                _uiState.value = AuthUiState.Success("로그인 성공했지만 사용자 정보를 가져오는데 실패했습니다.")
            }
        }
    }
    
    // Room DB에 사용자 정보 저장
    private fun saveUserToDatabase(userInfo: UserInfo) {
        viewModelScope.launch {
            try {
                userRepository.saveUser(userInfo)
                Log.d(TAG, "Room DB에 사용자 정보 저장 성공")
            } catch (e: Exception) {
                Log.e(TAG, "Room DB에 사용자 정보 저장 실패", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "로그아웃 시작")
                try {
                    userRepository.logout()
                    Log.d(TAG, "서버 로그아웃 성공")
                } catch (e: Exception) {
                    Log.e(TAG, "서버 로그아웃 실패, 로컬만 삭제 진행", e)
                }

                // 3. 토큰 삭제는 서버 로그아웃 이후에 진행
                tokenManager.clearTokens()
                Log.d(TAG, "로컬 토큰 삭제 완료")

                // 4. 로컬 사용자 정보 삭제 (서버 로그아웃 성공 여부와 관계없이)
                try {
                    userRepository.localLogout()
                    Log.d(TAG, "로컬 사용자 정보 삭제 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "로컬 사용자 정보 삭제 실패", e)
                }

                // 5. 상태 초기화
                _userState.value = null
                _tokenState.value = null
                _uiState.value = AuthUiState.Initial
                Log.d(TAG, "상태 초기화 완료")

                // 6. 네비게이션 이벤트 발생
                Log.d(TAG, "로그인 화면으로 이동 이벤트 발생 시도")
                _navigationEvent.emit(NavigationEvent.NavigateToLogin)
                Log.d(TAG, "로그인 화면으로 이동 이벤트 발생 완료")

            } catch (e: Exception) {
                Log.e(TAG, "로그아웃 과정 중 오류 발생", e)

                // 실패해도 상태 초기화
                _userState.value = null
                _tokenState.value = null
                _uiState.value = AuthUiState.Initial

                // 토큰 및 로컬 데이터 정리 시도
                try {
                    tokenManager.clearTokens()
                    Log.d(TAG, "실패 복구: 토큰 삭제 완료")
                } catch (ex: Exception) {
                    Log.e(TAG, "실패 복구: 토큰 삭제 실패", ex)
                }

                try {
                    userRepository.logout() // 파라미터 없는 메서드 호출
                    Log.d(TAG, "실패 복구: 로컬 사용자 정보 삭제 완료")
                } catch (ex: Exception) {
                    Log.e(TAG, "실패 복구: 로컬 사용자 정보 삭제 실패", ex)
                }

                // 어떤 상황에서도 로그인 화면으로 이동
                try {
                    Log.d(TAG, "실패 복구: 로그인 화면으로 이동 이벤트 발생 시도")
                    _navigationEvent.emit(NavigationEvent.NavigateToLogin)
                    Log.d(TAG, "실패 복구: 로그인 화면으로 이동 이벤트 발생 완료")
                } catch (ex: Exception) {
                    Log.e(TAG, "실패 복구: 네비게이션 이벤트 발생 실패", ex)
                }
            }
        }
    }
    //회원탈퇴
    fun deleteAccount() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "회원탈퇴 시작")

                // 서버에 회원탈퇴 요청
                val result = userRepository.deleteAccount()

                if (result.isSuccess) {
                    Log.d(TAG, "회원탈퇴 성공")

                    // 토큰 삭제
                    tokenManager.clearTokens()
                    Log.d(TAG, "로컬 토큰 삭제 완료")

                    // 상태 초기화
                    _userState.value = null
                    _tokenState.value = null
                    _uiState.value = AuthUiState.Initial
                    Log.d(TAG, "상태 초기화 완료")

                    // 로그인 화면으로 이동
                    Log.d(TAG, "로그인 화면으로 이동 이벤트 발생")
                    _navigationEvent.emit(NavigationEvent.NavigateToLogin)
                } else {
                    // 실패 처리
                    Log.e(TAG, "회원탈퇴 실패: ${result.exceptionOrNull()?.message}")
                    _uiState.value = AuthUiState.Error("회원탈퇴에 실패했습니다: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "회원탈퇴 중 오류 발생", e)
                _uiState.value = AuthUiState.Error("회원탈퇴 중 오류가 발생했습니다: ${e.message}")

                // 오류 발생해도 로그인 화면으로 이동 시도
                try {
                    _navigationEvent.emit(NavigationEvent.NavigateToLogin)
                } catch (ex: Exception) {
                    Log.e(TAG, "네비게이션 이벤트 발생 실패", ex)
                }
            }
        }
    }
    
    // 로그인 성공 상태로 설정
    fun setLoginSuccess() {
        _uiState.value = AuthUiState.Success("로그인 성공")
    }
}

// UI 상태를 표현하는 sealed 클래스
sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}