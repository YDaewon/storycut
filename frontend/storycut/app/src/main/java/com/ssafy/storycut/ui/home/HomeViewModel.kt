package com.ssafy.storycut.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.storycut.data.api.model.room.RoomDto
import com.ssafy.storycut.data.api.model.member.MemberDto
import com.ssafy.storycut.data.api.model.room.CreateRoomRequest
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.data.repository.RoomRepository
import com.ssafy.storycut.data.repository.S3Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val tokenManager: TokenManager,
    private val s3Repository: S3Repository,
) : ViewModel() {

    // 내 공유방 목록 LiveData
    private val _myRooms = MutableLiveData<List<RoomDto>>()
    val myRooms: LiveData<List<RoomDto>> = _myRooms

    // 단일 공유방 상세 정보 LiveData
    private val _roomDetail = MutableLiveData<RoomDto>()
    val roomDetail: LiveData<RoomDto> = _roomDetail

    // 공유방 참여자 목록 LiveData
    private val _roomMembers = MutableLiveData<List<MemberDto>>()
    val roomMembers: LiveData<List<MemberDto>> = _roomMembers

    // 초대 코드 LiveData
    private val _inviteCode = MutableLiveData<String>()
    val inviteCode: LiveData<String> = _inviteCode

    // 로딩 상태 LiveData
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 오류 메시지 LiveData
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // 새로 생성된 방 ID
    private val _createdRoomId = MutableLiveData<String>()
    val createdRoomId: LiveData<String> = _createdRoomId

    // 새로 입장한 방 ID
    private val _enteredRoomId = MutableLiveData<String>()
    val enteredRoomId: LiveData<String> = _enteredRoomId

    // 이미지 업로드 상태 추적
    private val _uploadingImage = MutableLiveData<Boolean>()
    val uploadingImage: LiveData<Boolean> = _uploadingImage

    // 내 공유방 목록 조회
    fun getMyRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = tokenManager.accessToken.first()
                if (token == null) {
                    _error.value = "인증 토큰이 없습니다. 다시 로그인해주세요."
                    return@launch
                }

                val response = roomRepository.getMyRooms()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val rooms = response.body()?.result ?: emptyList()
                    _myRooms.value = rooms
                } else {
                    val errorMsg = response.body()?.message ?: "공유방 목록을 불러오는데 실패했습니다."
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadImage(uri: Uri): String {
        Log.d(TAG, "uploadImage: 이미지 업로드 시작, URI: $uri")
        _uploadingImage.value = true
        _error.value = "" // 이전 오류 메시지 초기화

        return try {
            val token = tokenManager.accessToken.first()
            if (token == null) {
                Log.e(TAG, "uploadImage: 토큰이 없음")
                _error.value = "인증 토큰이 없습니다. 다시 로그인해주세요."
                return "default_thumbnail"
            }

            Log.d(TAG, "uploadImage: S3 업로드 시작")
            try {
                val response = s3Repository.uploadRoomThumbNailImage(uri)
                Log.d(TAG, "uploadImage: 응답 받음, 코드: ${response.code()}")

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val result = response.body()?.result
                    val imageUrl = result?.url

                    if (imageUrl != null && imageUrl.isNotEmpty()) {
                        Log.d(TAG, "uploadImage: 성공, URL: $imageUrl")
                        imageUrl
                    } else {
                        Log.w(TAG, "uploadImage: URL이 없거나 비어 있음, 기본 썸네일 사용")
                        _error.value = "서버에서 이미지 URL이 반환되지 않았습니다."
                        "default_thumbnail"
                    }
                } else {
                    // 상세 오류 정보 수집
                    val errorCode = response.code()
                    val errorMsg = when {
                        response.body()?.message != null -> response.body()?.message
                        response.errorBody() != null -> try {
                            response.errorBody()?.string() ?: "알 수 없는 오류"
                        } catch (e: Exception) {
                            "응답 파싱 오류: ${e.message}"
                        }
                        else -> "이미지 업로드에 실패했습니다 (HTTP $errorCode)"
                    }

                    Log.e(TAG, "uploadImage: 업로드 실패, 코드: $errorCode, 메시지: $errorMsg")
                    _error.value = errorMsg ?: "이미지 업로드에 실패했습니다."
                    "default_thumbnail"
                }
            } catch (e: IOException) {
                // IO 예외 처리 (파일 접근, 네트워크 문제 등)
                Log.e(TAG, "uploadImage: IO 예외", e)
                _error.value = "이미지 파일 처리 중 오류: ${e.message}"
                "default_thumbnail"
            } catch (e: Exception) {
                // 기타 예외 처리
                Log.e(TAG, "uploadImage: API 호출 예외", e)
                _error.value = "API 호출 중 오류: ${e.message}"
                "default_thumbnail"
            }
        } catch (e: Exception) {
            // 최상위 예외 처리
            Log.e(TAG, "uploadImage: 예외 발생", e)
            _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            "default_thumbnail"
        } finally {
            _uploadingImage.value = false
            Log.d(TAG, "uploadImage: 완료")
        }
    }

    fun createRoom(request: CreateRoomRequest, imageUri: Uri? = null) {
        Log.d(TAG, "createRoom: 공유방 생성 시작, 이름: ${request.roomTitle}, 이미지 유무: ${imageUri != null}")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = "" // 이전 오류 메시지 초기화

            try {
                val token = tokenManager.accessToken.first()
                if (token == null) {
                    Log.e(TAG, "createRoom: 토큰이 없음")
                    _error.value = "인증 토큰이 없습니다. 다시 로그인해주세요."
                    return@launch
                }

                // 이미지가 있으면 먼저 업로드
                Log.d(TAG, "createRoom: 이미지 처리 시작")
                val finalRequest = if (imageUri != null) {
                    try {
                        Log.d(TAG, "createRoom: 이미지 업로드 시작")
                        val imageUrl = uploadImage(imageUri)
                        Log.d(TAG, "createRoom: 이미지 업로드 완료, URL: $imageUrl")

                        // 오류 메시지가 설정되었는지 확인
                        if (_error.value?.isNotEmpty() == true) {
                            // 사용자에게 알림
                            Log.w(TAG, "createRoom: 이미지 업로드 중 오류가 있었지만 기본 이미지로 계속 진행합니다")
                        }

                        request.copy(roomThumbnail = imageUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "createRoom: 이미지 업로드 중 예외 발생, 기본 이미지로 계속 진행", e)
                        _error.value = "이미지 업로드 중 오류가 발생했지만, 기본 이미지로 계속 진행합니다."
                        request.copy(roomThumbnail = "default_thumbnail")
                    }
                } else {
                    Log.d(TAG, "createRoom: 이미지 없음, 기본 썸네일 사용")
                    request.copy(roomThumbnail = "default_thumbnail")
                }

                Log.d(TAG, "createRoom: API 호출 시작")
                val response = roomRepository.createRoom(finalRequest)
                Log.d(TAG, "createRoom: API 응답 받음, 코드: ${response.code()}")

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    response.body()?.result?.let {
                        // 방 생성 성공 시 바로 생성된 방의 ID 설정
                        val roomId = it.roomId.toString()
                        Log.d(TAG, "createRoom: 성공, 방 ID: $roomId")
                        _createdRoomId.value = roomId

                        // 이미지 업로드 실패에 대한 사용자 알림이 필요한 경우 오류 메시지를 유지
                        if (_error.value?.contains("이미지") == true) {
                            // 오류 메시지 유지 (이미지 관련 오류)
                        } else {
                            _error.value = "" // 다른 오류는 지움
                        }

                        // 백그라운드에서 목록 업데이트 (UI 차단하지 않음)
                        Log.d(TAG, "createRoom: 백그라운드에서 목록 업데이트 시작")
                        viewModelScope.launch {
                            try {
                                val updateResponse = roomRepository.getMyRooms()
                                if (updateResponse.isSuccessful) {
                                    Log.d(TAG, "createRoom: 백그라운드 목록 업데이트 성공")
                                } else {
                                    Log.w(TAG, "createRoom: 백그라운드 목록 업데이트 응답 실패: ${updateResponse.code()}")
                                }
                            } catch (e: Exception) {
                                // 무시 - 목록 갱신이 실패해도 사용자 경험에 영향 없음
                                Log.w(TAG, "createRoom: 백그라운드 목록 업데이트 실패", e)
                            }
                        }
                    }
                } else {
                    val errorMsg = response.body()?.message
                        ?: response.errorBody()?.string()
                        ?: "공유방 생성에 실패했습니다 (HTTP ${response.code()})"
                    Log.e(TAG, "createRoom: 실패, $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "createRoom: 예외 발생", e)
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
                Log.d(TAG, "createRoom: 완료")
            }
        }
    }

    // 생성된 방 ID 초기화
    fun clearCreatedRoomId() {
        _createdRoomId.value = ""
    }

    // 입장한 방 ID 초기화
    fun clearEnteredRoomId() {
        _enteredRoomId.value = ""
    }

    // 공유방 상세 정보 조회
    fun getRoomDetail(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = roomRepository.getRoomDetail(roomId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    response.body()?.result?.let {
                        _roomDetail.value = it
                    }
                } else {
                    _error.value = response.body()?.message ?: "공유방 정보를 불러오는데 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 공유방 참여자 목록 조회
    fun getRoomMembers(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = roomRepository.getRoomMembers(roomId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _roomMembers.value = response.body()?.result ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "참여자 목록을 불러오는데 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 공유방 입장 (초대코드와 비밀번호로 2단계 프로세스 구현)
    fun enterRoom(inviteCode: String, password: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""

            try {
                // 1단계: 초대코드로 공유방 ID 조회
                val decodeResponse = roomRepository.decodeInviteCode(inviteCode)

                if (decodeResponse.isSuccessful && decodeResponse.body()?.isSuccess == true) {
                    val roomId = decodeResponse.body()?.result

                    if (roomId != null) {
                        // 2단계: 조회한 공유방 ID로 입장 (비밀번호 포함)
                        val enterResponse = roomRepository.enterRoom(roomId.toString(), password)

                        if (enterResponse.isSuccessful && enterResponse.body()?.isSuccess == true) {
                            // 공유방 입장 후 목록 다시 불러오기
                            getMyRooms()
                            // 입장한 방의 ID 설정
                            val enteredRoom = enterResponse.body()?.result
                            if (enteredRoom != null) {
                                _enteredRoomId.value = enteredRoom.roomId.toString()
                            } else {
                                _error.value = "방 정보를 불러올 수 없습니다."
                            }
                        } else {
                            _error.value = enterResponse.body()?.message ?: "공유방 입장에 실패했습니다."
                        }
                    } else {
                        _error.value = "유효하지 않은 초대코드입니다."
                    }
                } else {
                    _error.value = decodeResponse.body()?.message ?: "초대코드 확인에 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 공유방 나가기
    fun leaveRoom(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = roomRepository.leaveRoom(roomId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    // 공유방 나간 후 목록 다시 불러오기
                    getMyRooms()
                } else {
                    _error.value = response.body()?.message ?: "공유방 나가기에 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 공유방 삭제
    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = roomRepository.deleteRoom(roomId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    // 공유방 삭제 후 목록 다시 불러오기
                    getMyRooms()
                } else {
                    _error.value = response.body()?.message ?: "공유방 삭제에 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 오류 메시지 초기화
    fun clearError() {
        _error.value = ""
    }
}