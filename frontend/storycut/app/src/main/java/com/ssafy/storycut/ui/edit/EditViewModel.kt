package com.ssafy.storycut.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.data.repository.EditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: EditRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    // UI 상태 관련 변수들
    var videoSelected by mutableStateOf(false)
        private set
    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set
    var videoThumbnail by mutableStateOf<Bitmap?>(null)
        private set
    var hasMosaic by mutableStateOf(false)
        private set
    var applySubtitle by mutableStateOf(false)
        private set
    var hasBackgroundMusic by mutableStateOf(false)
        private set
    var promptText by mutableStateOf("")
        private set
    // 배경음악을 위한 별도의 프롬프트 변수 추가
    var musicPromptText by mutableStateOf("")
        private set
    var mosaicImages by mutableStateOf<List<Uri>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var videoTitle by mutableStateOf("")
        private set

    var autoMusic by mutableStateOf(false)
        private set
    var selectedVideoUrl by mutableStateOf<String?>(null)
        private set

    // 내 쇼츠에서 선택한 경우의 썸네일 URL 저장
    var selectedVideoThumbnailUrl by mutableStateOf<String?>(null)
        private set

    // 비디오 URL 설정 함수 수정 - 썸네일 URL도 함께 저장
    fun setSelectedVideoUrl(url: String, thumbnailUrl: String?) {
        selectedVideoUrl = url
        selectedVideoThumbnailUrl = thumbnailUrl
        videoSelected = true
    }

    // 배경 음악 자동 생성 옵션 설정 - 함수명 변경
    fun updateAutoMusic(auto: Boolean) {
        autoMusic = auto
    }

    fun updateVideoTitle(title: String) {
        videoTitle = title
    }

    // 배경 음악 설정 함수 - 옵션과 자동 설정 여부를 함께 처리
    fun setBackgroundMusic(enable: Boolean, auto: Boolean = false) {
        hasBackgroundMusic = enable
        autoMusic = auto
        // 자동 생성 모드일 경우 프롬프트 초기화
        if (auto) {
            musicPromptText = ""
        }
    }


    // 이벤트 플로우
    private val _events = MutableSharedFlow<EditEvent>()
    val events: SharedFlow<EditEvent> = _events.asSharedFlow()

    // 비디오 선택
    fun setSelectedVideo(uri: Uri) {
        selectedVideoUri = uri
        selectedVideoUrl = null // URI를 설정할 때 URL 초기화
        selectedVideoThumbnailUrl = null // URI를 설정할 때 URL 썸네일 초기화
        videoSelected = true
        loadVideoThumbnail(uri)
    }

    // 비디오 썸네일 로드
    private fun loadVideoThumbnail(uri: Uri) {
        viewModelScope.launch {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                videoThumbnail = retriever.getFrameAtTime(0)
                retriever.release()
            } catch (e: Exception) {
                Log.e("EditViewModel", "썸네일 로드 실패", e)
                videoThumbnail = null
            }
        }
    }

    // 모자이크 옵션 토글
    fun toggleMosaic(enable: Boolean) {
        hasMosaic = enable
        if (!enable) {
            mosaicImages = emptyList()
        }
    }

    // 한국어 자막 옵션 토글
    fun toggleKoreanSubtitle(enable: Boolean) {
        applySubtitle = enable
    }

    // 배경 음악 옵션 토글
    fun toggleBackgroundMusic(enable: Boolean) {
        hasBackgroundMusic = enable
    }

    // 프롬프트 텍스트 업데이트
    fun updatePromptText(text: String) {
        promptText = text
    }

    // 배경음악 프롬프트 텍스트 업데이트 함수 추가
    fun updateMusicPromptText(text: String) {
        musicPromptText = text
    }

    // 모자이크 이미지 추가
    fun addMosaicImage(uri: Uri) {
        if (mosaicImages.size < 2) {
            mosaicImages = mosaicImages + uri
        }
    }

    // 모자이크 이미지 제거
    fun removeMosaicImage(index: Int) {
        mosaicImages = mosaicImages.filterIndexed { i, _ -> i != index }
    }

    // 오류 메시지 초기화
    fun clearError() {
        error = null
    }

    // 편집 처리 시작
    fun processEditing() {
        // 비디오 URL이나 URI 둘 중 하나가 있는지 확인
        if (selectedVideoUri == null && selectedVideoUrl == null) {
            error = "비디오를 선택해주세요"
            return
        }
        if (videoTitle.isBlank()) {
            error = "비디오 제목을 입력해주세요"
            return
        }

        isLoading = true
        error = null

        viewModelScope.launch {
            try {
                val videoId: Long

                // 선택된 비디오 소스에 따라 다른 처리
                if (selectedVideoUrl != null) {
                    // 이미 서버에 있는 비디오 URL 처리
                    val videoRegisterResult = repository.registerExistingVideo(
                        videoUrl = selectedVideoUrl!!,
                        videoTitle = videoTitle,
                        thumbnailUrl = selectedVideoThumbnailUrl // 내 쇼츠에서 가져온 썸네일 URL 사용
                    )

                    if (videoRegisterResult.isFailure) {
                        throw videoRegisterResult.exceptionOrNull() ?: Exception("비디오 등록 실패")
                    }

                    videoId = videoRegisterResult.getOrNull() ?: throw Exception("등록된 비디오 ID를 찾을 수 없습니다")
                } else {
                    // 로컬 갤러리 URI 처리 (기존 코드)
                    val videoUploadResult = repository.uploadVideoWithPresignedUrl(
                        selectedVideoUri!!,
                        videoTitle,
                        videoThumbnail
                    )

                    if (videoUploadResult.isFailure) {
                        throw videoUploadResult.exceptionOrNull() ?: Exception("비디오 업로드 실패")
                    }

                    videoId = videoUploadResult.getOrNull() ?: throw Exception("업로드된 비디오 ID를 찾을 수 없습니다")
                }

                // 2. 모자이크 이미지 업로드 (필요시)
                var imageUrls = listOf<String>()
                if (hasMosaic && mosaicImages.isNotEmpty()) {
                    val imageUploadResult = repository.uploadImages(mosaicImages)
                    if (imageUploadResult.isSuccess) {
                        imageUrls = imageUploadResult.getOrNull() ?: emptyList()
                    }
                }

                // 3. 비디오 처리 요청 - 배경 음악 프롬프트와 일반 프롬프트 분리
                val processResult = repository.processVideo(
                    prompt = promptText.takeIf { it.isNotBlank() },
                    videoId = videoId,
                    imageUrls = imageUrls,
                    videoTitle = videoTitle,
                    applySubtitle = applySubtitle,
                    musicPrompt = if (hasBackgroundMusic && !autoMusic) musicPromptText.takeIf { it.isNotBlank() } else null,
                    autoMusic = hasBackgroundMusic && autoMusic // 자동 음악 생성 여부 전달
                )

                isLoading = false
                Log.d("EditViewModel", "영상 처리 결과: ${processResult.isSuccess}")

                if (processResult.isSuccess) {
                    _events.emit(EditEvent.Processing(videoId.toString()))
                } else {
                    error = processResult.exceptionOrNull()?.message ?: "영상 처리 실패"
                    _events.emit(EditEvent.Error(error ?: "알 수 없는 오류"))
                }

            } catch (e: Exception) {
                Log.e("EditViewModel", "영상 처리 실패", e)
                isLoading = false
                error = e.message ?: "오류가 발생했습니다"
                _events.emit(EditEvent.Error(error ?: "알 수 없는 오류"))
            }
        }
    }

    // 상태 초기화 함수 - 추가 변수도 초기화 처리
    fun resetState() {
        // 비디오 관련 상태 초기화
        selectedVideoUri = null
        selectedVideoUrl = null
        selectedVideoThumbnailUrl = null
        videoThumbnail = null
        videoSelected = false

        // 제목과 프롬프트 초기화
        videoTitle = ""
        promptText = ""

        // 옵션 초기화
        hasMosaic = false
        applySubtitle = false
        hasBackgroundMusic = false
        autoMusic = false // autoMusic 초기화 추가

        // 모자이크 이미지 및 음악 프롬프트 초기화
        mosaicImages = emptyList()
        musicPromptText = ""
    }

    // 이벤트 클래스
    sealed class EditEvent {
        data class Success(val videoId: String, val videoData: VideoDto) : EditEvent()
        data class Processing(val videoId: String) : EditEvent()
        data class Error(val message: String) : EditEvent()
    }
}