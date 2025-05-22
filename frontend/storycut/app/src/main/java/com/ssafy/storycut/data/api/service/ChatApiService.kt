package com.ssafy.storycut.data.api.service

import com.ssafy.storycut.data.api.model.BaseResponse
import com.ssafy.storycut.data.api.model.chat.ChatDto
import com.ssafy.storycut.data.api.model.chat.ChatMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {

    // 공유방 비디오 업로드
    @POST("chat/")
    suspend fun uploadRoomVideo(
        @Query("roomId") roomId: Long,
        @Body chatMessage: ChatMessageRequest,
    ): Response<BaseResponse<ChatDto>>

    // 공유방 비디오 목록 조회
    @GET("chat/{roomId}")
    suspend fun getRoomVideos(
        @Path("roomId") roomId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<BaseResponse<List<ChatDto>>>

    // 공유방 비디오 상세 조회 (추가)
    @GET("chat/detail/{chatId}")
    suspend fun getChatDetail(
        @Path("chatId") chatId: String): Response<BaseResponse<ChatDto>>

    // 공유방 비디오 삭제
    @DELETE("chat/{chatId}")
    suspend fun deleteChat(
        @Path("chatId") chatId: String): Response<BaseResponse<Boolean>>
}