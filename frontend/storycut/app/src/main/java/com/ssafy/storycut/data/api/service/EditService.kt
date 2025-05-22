package com.ssafy.storycut.data.api.service

import com.ssafy.storycut.data.api.model.*
import com.ssafy.storycut.data.api.model.Tumbnail.ThumbNailUploadResponse
import com.ssafy.storycut.data.api.model.edit.ImageUploadResponse
import com.ssafy.storycut.data.api.model.edit.MosaicRequest
import com.ssafy.storycut.data.api.model.edit.PresignedUrlResponse
import com.ssafy.storycut.data.api.model.edit.VideoProcessRequest
import com.ssafy.storycut.data.api.model.edit.VideoUploadRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface EditService {

    // 이미지 업로드 API - 여러 파일 업로드 가능
    @Multipart
    @POST("upload/images")
    suspend fun uploadImages(@Part files: List<MultipartBody.Part>):
            Response<BaseResponse<ImageUploadResponse>>

    // Azure Blob Presigned URL 발급 API
    @GET("presigned/presigned-url")
    suspend fun getPresignedUrl(
        @Query("original_filename") originalFilename: String
    ): Response<PresignedUrlResponse>

    // Azure 업로드 후 영상 등록 API
    @POST("upload/videos")
    suspend fun uploadVideo(
        @Header("Authorization") authorization: String,
        @Body request: VideoUploadRequest
    ): Response<BaseResponse<VideoDto>>

    // 룸 썸네일 이미지 업로드 API
    @Multipart
    @POST("upload/room-thumbnails")
    suspend fun uploadRoomThumbNailImage(@Part file: MultipartBody.Part):
            Response<BaseResponse<ThumbNailUploadResponse>>

    // 영상 처리 API (요약 + 모자이크 + 자막)
    @POST("videos")
    suspend fun processVideo(
        @Header("authorization") authorization: String,
        @Header("device-token") deviceToken: String,
        @Body request: VideoProcessRequest
    ): Response<BaseResponse<VideoDto>>

    // 모자이크 API
    @POST("mosaic")
    suspend fun applyMosaic(
        @Header("authorization") authorization: String,
        @Header("device-token") deviceToken: String,
        @Body request: MosaicRequest
    ): Response<BaseResponse<VideoDto>>
}