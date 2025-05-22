package com.ssafy.storycut.data.repository

import com.ssafy.storycut.data.api.model.BaseResponse
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.data.api.service.VideoApiService
import retrofit2.Response
import javax.inject.Inject

class VideoRepository @Inject constructor(
    private val videoApiService: VideoApiService
) {

    suspend fun getMyVideos(isOriginal: Boolean = false): Response<BaseResponse<List<VideoDto>>> {
        return videoApiService.getMyVideos(isOriginal)
    }

    // 단일 비디오 상세 정보
    suspend fun getVideoDetail(videoId: String): Response<BaseResponse<VideoDto>> {
        return videoApiService.getMyVideo(videoId)
    }
    
    // 공유방 비디오 호출
    suspend fun getRoomVideos(roomId: String): Response<BaseResponse<List<VideoDto>>> {
        return videoApiService.getRoomVideos(roomId)
    }
}