package com.ssafy.storycut.ui.room

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.storycut.data.api.model.room.EditRoomDto
import com.ssafy.storycut.data.api.model.member.MemberDto
import com.ssafy.storycut.data.api.model.Tumbnail.ThumbnailDto
import com.ssafy.storycut.data.api.model.credential.UserInfo
import com.ssafy.storycut.data.api.model.chat.ChatDto
import com.ssafy.storycut.data.api.model.chat.ChatMessageRequest
import com.ssafy.storycut.data.api.model.room.RoomDto
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.data.repository.RoomRepository
import com.ssafy.storycut.data.repository.S3Repository
import com.ssafy.storycut.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val s3Repository: S3Repository, // S3Repository 주입 추가
    private val tokenManager: TokenManager // TokenManager 주입 추가
) : ViewModel() {
    private val TAG = "Room"
    private val _roomDetail = MutableStateFlow<RoomDto?>(null)
    val roomDetail: StateFlow<RoomDto?> = _roomDetail

    private val _roomMembers = MutableStateFlow<List<MemberDto>>(emptyList())
    val roomMembers: StateFlow<List<MemberDto>> = _roomMembers

    private val _inviteCode = MutableStateFlow("")
    val inviteCode: StateFlow<String> = _inviteCode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _inviteCodeCreationTime = MutableStateFlow<Long>(0)
    val inviteCodeCreationTime: StateFlow<Long> = _inviteCodeCreationTime

    // 비디오 업로드 성공 여부
    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess

    // 썸네일 업데이트 성공 여부
    private val _thumbnailUpdateSuccess = MutableStateFlow(false)
    val thumbnailUpdateSuccess: StateFlow<Boolean> = _thumbnailUpdateSuccess

    // 비디오 목록 상태
    private val _roomVideos = MutableStateFlow<List<ChatDto>>(emptyList())
    val roomVideos: StateFlow<List<ChatDto>> = _roomVideos

    // 페이징 관련 상태
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _hasMoreVideos = MutableStateFlow(true)
    val hasMoreVideos: StateFlow<Boolean> = _hasMoreVideos

    // 비디오 목록 로딩 상태
    private val _isVideosLoading = MutableStateFlow(false)
    val isVideosLoading: StateFlow<Boolean> = _isVideosLoading

    // 단일 비디오 상세 정보를 위한 상태
    private val _currentVideoDetail = MutableStateFlow<ChatDto?>(null)
    val currentVideoDetail: StateFlow<ChatDto?> = _currentVideoDetail

    // 비디오 상세 정보 로드 중 상태
    private val _isVideoDetailLoading = MutableStateFlow(false)
    val isVideoDetailLoading: StateFlow<Boolean> = _isVideoDetailLoading

    // 비디오 상세 정보 로드 에러
    private val _videoDetailError = MutableStateFlow<String?>(null)
    val videoDetailError: StateFlow<String?> = _videoDetailError

    // 비디오 업로더 정보를 위한 상태
    private val _videoUploaderInfo = MutableStateFlow<UserInfo?>(null)
    val videoUploaderInfo: StateFlow<UserInfo?> = _videoUploaderInfo

    // 비디오 업로더 정보 로딩 상태
    private val _isUploaderInfoLoading = MutableStateFlow(false)
    val isUploaderInfoLoading: StateFlow<Boolean> = _isUploaderInfoLoading

    // 업로더 정보 로드 에러
    private val _uploaderInfoError = MutableStateFlow<String?>(null)
    val uploaderInfoError: StateFlow<String?> = _uploaderInfoError


    /**
     * 비디오 상세 정보 조회
     * @param chatId 채팅 메시지 ID
     */
    fun getVideoDetail(chatId: String) {
        viewModelScope.launch {
            _isVideoDetailLoading.value = true
            _videoDetailError.value = null
            _isUploaderInfoLoading.value = true
            _uploaderInfoError.value = null

            try {
                Log.d(TAG, "비디오 상세 정보 조회 시작: chatId=$chatId")
                val response = roomRepository.getChatDetail(chatId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val videoDetail = response.body()?.result
                    _currentVideoDetail.value = videoDetail
                    Log.d(TAG, "비디오 상세 정보 조회 성공: ${videoDetail?.title}")

                    // 비디오 업로더 정보 조회 (memberId가 있는 경우)
                    videoDetail?.senderId?.let { senderId ->
                        fetchUploaderInfo(senderId)
                    } ?: run {
                        _uploaderInfoError.value = "업로더 정보가 없습니다."
                        _isUploaderInfoLoading.value = false
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "비디오 정보를 불러오는데 실패했습니다."
                    _videoDetailError.value = errorMsg
                    Log.e(TAG, "비디오 상세 정보 조회 실패: $errorMsg, 코드: ${response.code()}")
                    _isUploaderInfoLoading.value = false
                }
            } catch (e: Exception) {
                _videoDetailError.value = "네트워크 오류: ${e.message}"
                Log.e(TAG, "비디오 상세 정보 조회 중 예외 발생", e)
                _isUploaderInfoLoading.value = false
            } finally {
                _isVideoDetailLoading.value = false
            }
        }
    }

    /**
     * 업로더 정보 조회
     * @param memberId 멤버 ID
     */
    private suspend fun fetchUploaderInfo(memberId: Long) {
        try {
            val result = userRepository.getMemberById(memberId)
            result.onSuccess { userInfo ->
                _videoUploaderInfo.value = userInfo
                Log.d(TAG, "업로더 정보 조회 성공: ${userInfo.nickname}")
            }.onFailure { exception ->
                _uploaderInfoError.value = "업로더 정보 조회 실패: ${exception.message}"
                Log.e(TAG, "업로더 정보 조회 실패", exception)
            }
        } catch (e: Exception) {
            _uploaderInfoError.value = "업로더 정보 조회 중 오류 발생: ${e.message}"
            Log.e(TAG, "업로더 정보 조회 중 예외 발생", e)
        } finally {
            _isUploaderInfoLoading.value = false
        }
    }

    // 비디오 및 업로더 상세 상태 초기화
    fun clearVideoDetail() {
        _currentVideoDetail.value = null
        _videoDetailError.value = null
        _videoUploaderInfo.value = null
        _uploaderInfoError.value = null
    }

    // 공유방 상세 정보 가져오기
    fun getRoomDetail(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""

            try {
                Log.d("RoomViewModel", "Loading room details for roomId: $roomId")
                val response = roomRepository.getRoomDetail(roomId)

                Log.d("RoomViewModel", "Response code: ${response.code()}")
                if (response.isSuccessful) {
                    Log.d("RoomViewModel", "Response successful: ${response.body()}")
                } else {
                    Log.e("RoomViewModel", "Error response: ${response.errorBody()?.string()}")
                }

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _roomDetail.value = response.body()?.result
                    Log.d("RoomViewModel", "Room detail loaded: ${_roomDetail.value}")
                } else {
                    _error.value = response.body()?.message ?: "공유방 정보를 불러오는데 실패했습니다."
                    Log.e("RoomViewModel", "Failed to load room detail: ${_error.value}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
                Log.e("RoomViewModel", "Exception loading room detail", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 썸네일 업데이트 함수
    fun updateRoomThumbnail(roomId: String, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            _thumbnailUpdateSuccess.value = false

            try {
                Log.d("RoomViewModel", "Updating room thumbnail for roomId: $roomId")

                // 이미지 업로드 먼저 진행
                val imageUrl = uploadRoomThumbnail(imageUri)
                Log.d(TAG,"url : ${imageUrl}")
                if (_error.value.isNotEmpty()) {
                    // 이미지 업로드 실패 시 중단
                    return@launch
                }

                val thumbnailDto = ThumbnailDto(thumbnail = imageUrl)

                // 방 정보 업데이트 API 호출 (수정된 부분)
                val response = roomRepository.updateRoomThumbnail(roomId.toLong(), thumbnailDto)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    // 성공한 경우 응답으로 받은 방 정보로 바로 업데이트
                    response.body()?.result?.let { updatedRoom ->
                        _roomDetail.value = updatedRoom
                    }

                    _thumbnailUpdateSuccess.value = true
                    Log.d("RoomViewModel", "Room thumbnail updated successfully")
                } else {
                    _error.value = response.body()?.message ?: "썸네일 업데이트에 실패했습니다."
                    Log.e("RoomViewModel", "Failed to update thumbnail: ${_error.value}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
                Log.e("RoomViewModel", "Exception updating thumbnail", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // S3 썸네일 이미지 업로드 함수
    private suspend fun uploadRoomThumbnail(uri: Uri): String {
        Log.d("RoomViewModel", "Uploading thumbnail image: $uri")

        try {
            // 토큰 가져오기
            val token = tokenManager.accessToken.first()
            if (token == null) {
                Log.e("RoomViewModel", "No token available")
                _error.value = "인증 토큰이 없습니다. 다시 로그인해주세요."
                return "default_thumbnail"
            }

            // S3 업로드 API 호출
            val response = s3Repository.uploadRoomThumbNailImage(uri)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val result = response.body()?.result
                val imageUrl = result?.url

                if (imageUrl != null && imageUrl.isNotEmpty()) {
                    Log.d("RoomViewModel", "Thumbnail uploaded successfully: $imageUrl")
                    return imageUrl
                } else {
                    Log.e("RoomViewModel", "Image URL is empty or null")
                    _error.value = "서버에서 이미지 URL이 반환되지 않았습니다."
                    return "default_thumbnail"
                }
            } else {
                val errorMsg = response.body()?.message ?: "이미지 업로드에 실패했습니다."
                Log.e("RoomViewModel", "Failed to upload image: $errorMsg")
                _error.value = errorMsg
                return "default_thumbnail"
            }
        } catch (e: Exception) {
            Log.e("RoomViewModel", "Exception uploading image", e)
            _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            return "default_thumbnail"
        }
    }

    // 공유방 멤버 목록 가져오기
    fun getRoomMembers(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""

            try {
                val response = roomRepository.getRoomMembers(roomId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _roomMembers.value = response.body()?.result ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "멤버 목록을 불러오는데 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // createInviteCode 함수
    fun createInviteCode(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""

            try {
                val response = roomRepository.createInviteCode(roomId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _inviteCode.value = response.body()?.result ?: ""
                    _inviteCodeCreationTime.value = System.currentTimeMillis() // 코드 생성 시간 저장
                } else {
                    _error.value = response.body()?.message ?: "초대코드 생성에 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadShort(
        roomId: String,
        videoUrl: String,
        title: String,
        thumbnailUrl: String,
        videoId: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            _uploadSuccess.value = false

            try {
                Log.d("RoomViewModel", "Uploading short for roomId: $roomId with title: $title")

                // 업로드할 메시지 요청 객체 생성
                val chatMessage = ChatMessageRequest(
                    videoId = videoId,
                    title = title,
                    mediaUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl
                )

                // 방 ID를 Long으로 변환
                val roomIdLong = try {
                    roomId.toLong()
                } catch (e: Exception) {
                    _error.value = "잘못된 방 ID 형식입니다."
                    Log.e("RoomViewModel", "Invalid room ID format", e)
                    return@launch
                }

                // 리포지토리를 통해 API 호출
                val response = roomRepository.uploadRoomVideo(roomIdLong, chatMessage)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d("RoomViewModel", "Short upload successful: ${response.body()}")
                    _uploadSuccess.value = true
                } else {
                    _error.value = response.body()?.message ?: "쇼츠 업로드에 실패했습니다."
                    Log.e("RoomViewModel", "Failed to upload short: ${_error.value}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
                Log.e("RoomViewModel", "Exception uploading short", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 공유방 비디오 목록 조회
    fun getRoomVideos(roomId: String, refresh: Boolean = false) {
        viewModelScope.launch {
            _isVideosLoading.value = true
            _error.value = ""

            // 새로고침인 경우 페이지를 0으로 초기화
            if (refresh) {
                _currentPage.value = 0
                _roomVideos.value = emptyList()
                _hasMoreVideos.value = true
            }

            // 더 이상 불러올 데이터가 없는 경우 중단
            if (!_hasMoreVideos.value && !refresh) {
                _isVideosLoading.value = false
                return@launch
            }

            try {
                // 방 ID를 Long으로 변환
                val roomIdLong = try {
                    roomId.toLong()
                } catch (e: Exception) {
                    _error.value = "잘못된 방 ID 형식입니다."
                    Log.e("RoomViewModel", "Invalid room ID format", e)
                    return@launch
                }

                Log.d("RoomViewModel", "Loading videos for roomId: $roomId, page: ${_currentPage.value}")
                val response = roomRepository.getRoomVideos(
                    roomId = roomIdLong,
                    page = _currentPage.value,
                    size = 10
                )

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val videos = response.body()?.result ?: emptyList()

                    // 데이터가 더 있는지 여부 확인 (10개 미만이면 더 이상 데이터가 없음)
                    _hasMoreVideos.value = videos.size == 10

                    // 기존 목록에 새로 불러온 항목 추가 (페이지 > 0인 경우)
                    if (_currentPage.value > 0 && !refresh) {
                        _roomVideos.value = _roomVideos.value + videos
                    } else {
                        // 첫 페이지이거나 새로고침인 경우 새 목록으로 대체
                        _roomVideos.value = videos
                    }

                    // 성공적으로 로드했으면 다음 페이지 준비
                    if (videos.isNotEmpty()) {
                        _currentPage.value = _currentPage.value + 1
                    }

                    Log.d("RoomViewModel", "Loaded ${videos.size} videos, total: ${_roomVideos.value.size}")
                } else {
                    _error.value = response.body()?.message ?: "비디오 목록을 불러오는데 실패했습니다."
                    Log.e("RoomViewModel", "Failed to load videos: ${_error.value}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
                Log.e("RoomViewModel", "Exception loading videos", e)
            } finally {
                _isVideosLoading.value = false
            }
        }
    }


    // 방 나가기 함수 추가
    fun leaveRoom(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""

            try {

                val response = roomRepository.leaveRoom(roomId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                } else {
                    _error.value = response.body()?.message ?: "방 나가기에 실패했습니다."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateRoom(roomDto: RoomDto, password: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""

            try {
                // EditRoomDto 객체 생성 - 수정된 형태에 맞춤
                val editRoomDto = EditRoomDto(
                    roomTitle = roomDto.roomTitle,
                    roomContext = roomDto.roomContext,
                    roomPassword = if (roomDto.hasPassword) password else null
                )

                val response = roomRepository.updateRoom(roomDto.roomId, editRoomDto)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    // 성공한 경우 응답으로 받은 방 정보로 업데이트
                    response.body()?.result?.let { updatedRoom ->
                        _roomDetail.value = updatedRoom
                    }
                    Log.d(TAG, "방 정보 업데이트 성공")
                } else {
                    _error.value = response.body()?.message ?: "방 정보 수정에 실패했습니다."
                    Log.e(TAG, "방 정보 업데이트 실패: ${_error.value}")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "네트워크 오류가 발생했습니다."
                Log.e(TAG, "방 정보 업데이트 중 예외 발생", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    // 비디오 목록 새로고침
    fun refreshRoomVideos(roomId: String) {
        getRoomVideos(roomId, true)
    }

    // 더 많은 비디오 불러오기
    fun loadMoreVideos(roomId: String) {
        if (!_isVideosLoading.value && _hasMoreVideos.value) {
            getRoomVideos(roomId)
        }
    }

    // 에러 메시지 초기화
    fun clearError() {
        _error.value = ""
    }

    // 업로드 상태 초기화
    fun resetUploadState() {
        _uploadSuccess.value = false
    }

    // 썸네일 업데이트 상태 초기화
    fun resetThumbnailUpdateState() {
        _thumbnailUpdateSuccess.value = false
    }
}