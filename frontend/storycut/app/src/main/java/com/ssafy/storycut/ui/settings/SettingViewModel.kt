package com.ssafy.storycut.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.storycut.data.api.model.credential.UserInfo
import com.ssafy.storycut.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // 현재 사용자 정보 관련 상태
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser

    // 닉네임만 별도로 제공하는 Flow
    val currentUserNickname: StateFlow<String?> = _currentUser.map { it?.nickname }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    // 닉네임 업데이트 결과
    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult: StateFlow<UpdateResult?> = _updateResult

    // 현재 사용자 정보 가져오기
    fun fetchCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { userInfo ->
                _currentUser.value = userInfo
            }
        }
    }

    // 닉네임 업데이트
    fun updateNickname(newNickname: String) {
        viewModelScope.launch {
            try {
                // UserRepository에 추가할 updateUserInfo 메서드 호출
                val result = userRepository.updateUserInfo(newNickname)

                result.fold(
                    onSuccess = { updatedUser ->
                        _currentUser.value = updatedUser
                        _updateResult.value = UpdateResult.Success(updatedUser.nickname)
                    },
                    onFailure = { exception ->
                        _updateResult.value = UpdateResult.Error(exception.message ?: "닉네임 업데이트에 실패했습니다.")
                    }
                )
            } catch (e: Exception) {
                _updateResult.value = UpdateResult.Error(e.message ?: "닉네임 업데이트 중 오류가 발생했습니다.")
            }
        }
    }

    // 닉네임 업데이트 결과 타입
    sealed class UpdateResult {
        data class Success(val nickname: String) : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}