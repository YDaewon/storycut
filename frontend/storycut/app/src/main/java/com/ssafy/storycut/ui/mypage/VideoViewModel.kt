package com.ssafy.storycut.ui.mypage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _myVideos = MutableStateFlow<List<VideoDto>>(emptyList())
    val myVideos: StateFlow<List<VideoDto>> = _myVideos

    // 비디오 상세 정보를 위한 StateFlow 추가
    private val _videoDetail = MutableStateFlow<VideoDto?>(null)
    val videoDetail: StateFlow<VideoDto?> = _videoDetail

    // 로딩 상태를 위한 StateFlow 추가
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 비디오 목록 가져오기 (최신순 정렬 추가)
    fun fetchMyVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = videoRepository.getMyVideos()
                Log.d("VideoViewModel", "응답 코드: ${response.code()}")

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    response.body()?.result?.let { videos ->
                        // 최신순 정렬 (videoId 기준 내림차순)
                        _myVideos.value = videos.sortedByDescending { it.videoId }
                        Log.d("VideoViewModel", "비디오 ${videos.size}개 로드됨, 최신순 정렬 완료")
                    }
                } else {
                    // 에러 처리 - 로그 추가
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.d("VideoViewModel", "비디오 목록 가져오기 실패 : ${errorMsg}")
                }
            } catch (e: Exception) {
                // 예외 처리 - 로그 추가
                Log.d("VideoViewModel", "비디오 목록 가져오기 에러 : ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // VideoViewModel.kt
    suspend fun getVideoDetail(videoId: String): VideoDto? {
        _isLoading.value = true
        try {
            Log.d("VideoViewModel", "비디오 상세 정보 요청: $videoId")
            val response = videoRepository.getVideoDetail(videoId)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val videoDto = response.body()?.result
                _videoDetail.value = videoDto
                Log.d("VideoViewModel", "비디오 상세 정보 로드 성공: $videoId, 제목: ${videoDto?.videoTitle}")
                return videoDto
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.d("VideoViewModel", "비디오 상세 정보 가져오기 실패: $errorMsg")
            }
        } catch (e: Exception) {
            Log.d("VideoViewModel", "비디오 상세 정보 가져오기 에러: ${e.message}")
        } finally {
            _isLoading.value = false
        }
        return null
    }

    // 비동기적으로 비디오 상세 정보를 가져오는 래퍼 함수 (ViewModelScope 사용)
    fun fetchVideoDetail(videoId: String) {
        viewModelScope.launch {
            try {
                getVideoDetail(videoId)
            } catch (e: Exception) {
                Log.d("VideoViewModel", "비디오 상세 정보 로드 에러: ${e.message}")
            }
        }
    }

    // 로컬 캐시에서 비디오 정보 찾기 (API 요청 없이 빠르게 정보 제공)
    fun getVideoFromCache(videoId: String): VideoDto? {
        // videoId를 Long으로 변환하여 비교
        val videoIdLong = videoId.toLongOrNull()
        return if (videoIdLong != null) {
            myVideos.value.find { it.videoId == videoIdLong }
        } else {
            // 변환할 수 없는 경우 null 반환
            null
        }
    }



    private val _roomVideos = MutableStateFlow<List<VideoDto>>(emptyList())

}