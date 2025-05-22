package com.ssafy.storycut.data.api.service

import com.ssafy.storycut.data.api.model.BaseResponse
import com.ssafy.storycut.data.api.model.VideoDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface VideoApiService {

    @GET("video")
    suspend fun getMyVideos(
        @Query("isOriginal") isOriginal: Boolean = false): Response<BaseResponse<List<VideoDto>>>

    @GET("video/{videoId}")
    suspend fun getMyVideo(@Path("videoId") videoId: String
    ): Response<BaseResponse<VideoDto>>

    @GET("video/room/{roomId}")
    suspend fun getRoomVideos(@Path("roomId") roomId: String): Response<BaseResponse<List<VideoDto>>>
}