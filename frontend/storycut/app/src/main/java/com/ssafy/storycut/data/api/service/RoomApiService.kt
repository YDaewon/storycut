package com.ssafy.storycut.data.api.service

import com.ssafy.storycut.data.api.model.BaseResponse
import com.ssafy.storycut.data.api.model.room.EditRoomDto
import com.ssafy.storycut.data.api.model.member.MemberDto
import com.ssafy.storycut.data.api.model.Tumbnail.ThumbnailDto
import com.ssafy.storycut.data.api.model.VideoShareDto
import com.ssafy.storycut.data.api.model.room.CreateRoomRequest
import com.ssafy.storycut.data.api.model.room.RoomDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RoomApiService {

    // 내 공유방 목록 조회
    @GET("room")
    suspend fun getMyRooms(): Response<BaseResponse<List<RoomDto>>>

    // 공유방 생성
    @POST("room")
    suspend fun createRoom(@Body roomDto: CreateRoomRequest): Response<BaseResponse<RoomDto>>

    // 공유방 삭제
    @DELETE("room")
    suspend fun deleteRoom(@Query("roomId") roomId: String): Response<BaseResponse<Boolean>>

    // 공유방 수정
    @PATCH("room")
    suspend fun updateRoom(
        @Query("roomId") roomId: Long,
        @Body editRoomDto: EditRoomDto
    ): Response<BaseResponse<RoomDto>>

    // 공유방 나가기
    @POST("room/leave")
    suspend fun leaveRoom(@Query("roomId") roomId: String): Response<BaseResponse<Boolean>>

    // 공유방 초대코드 생성
    @POST("room/invite")
    suspend fun createInviteCode(@Query("roomId") roomId: String): Response<BaseResponse<String>>

    // 공유방 입장
    @POST("room/enter")
    suspend fun enterRoom(
        @Query("roomId") roomId: String,
        @Query("password") password: String? = null
    ): Response<BaseResponse<RoomDto>>

    // 공유방 참여자 목록 조회
    @GET("room/members/{roomId}")
    suspend fun getRoomMembers(@Path("roomId") roomId: String): Response<BaseResponse<List<MemberDto>>>

    // 공유방 상세 정보 조회
    @GET("room/detail/{roomId}")
    suspend fun getRoomDetail(@Path("roomId") roomId: String): Response<BaseResponse<RoomDto>>

    // 초대코드로 공유방 ID 조회
    @GET("room/decode")
    suspend fun decodeInviteCode(@Query("inviteCode") inviteCode: String): Response<BaseResponse<String>>

    // 공유방 비디오 업로드
    @POST("chat/")
    suspend fun <VideoShareRequest> shareVideo(
        @Query("roomId") roomId: Long,
        @Body videoShareRequest: VideoShareRequest,
        
    ): Response<BaseResponse<VideoShareDto>>

    // 공유방 비디오 목록 조회
    @GET("chat/{roomId}")
    suspend fun getSharedVideos(
        @Path("roomId") roomId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        
    ): Response<BaseResponse<List<VideoShareDto>>>

    // 공유방 비디오 삭제
    @DELETE("chat/{chatId}")
    suspend fun deleteSharedVideo(
        @Path("chatId") chatId: String,
        
    ): Response<BaseResponse<Boolean>>

    // 공유방 썸네일 변경
//    @PATCH("room/thumbnail")
//    suspend fun updateRoomThumbnail(
//        @Query("roomId") roomId: Long,
//        @Body thumbnail: String
//    ): Response<BaseResponse<RoomDto>>

    @PATCH("room/thumbnail")
    suspend fun updateRoomThumbnail(
        @Query("roomId") roomId: Long,
        @Body thumbnailDto: ThumbnailDto
    ): Response<BaseResponse<RoomDto>>

}