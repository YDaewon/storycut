package com.ssafy.storycut.data.api.service

import com.ssafy.storycut.data.api.model.BaseResponse
import com.ssafy.storycut.data.api.model.credential.GoogleLoginRequest
import com.ssafy.storycut.data.api.model.credential.TokenDto
import com.ssafy.storycut.data.api.model.UpdateUserRequest
import com.ssafy.storycut.data.api.model.credential.UserInfo
import com.ssafy.storycut.data.api.model.credential.GooglePermissionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {
    @POST("auth/login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<BaseResponse<TokenDto>>

    @GET("members/detail")
    suspend fun getUserInfo(@Header("Authorization") authorization: String): Response<BaseResponse<UserInfo>>

    // 특정 회원 정보 조회 API 추가
    @GET("members/{memberId}")
    suspend fun getMemberById(
        @Path("memberId") memberId: Long
    ): Response<BaseResponse<UserInfo>>

    @DELETE("members")
    suspend fun deleteId(): Response<BaseResponse<Unit>>

    @PATCH("members/detail")
    suspend fun updateMemberDetail(@Body updateRequest: UpdateUserRequest): Response<BaseResponse<UserInfo>>

    // 토큰 갱신 API 추가
    @POST("auth/refresh")
    suspend fun refreshToken(@Body tokenDto: TokenDto): Response<BaseResponse<TokenDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<BaseResponse<Any>>

    @GET("auth/youtube/auth")
    suspend fun getYouTubeAuthUrl(@Header("Authorization") authorization: String): Response<BaseResponse<GooglePermissionResponse>>

    @POST("auth/google-refresh")
    suspend fun refreshGoogleToken(@Header("Authorization") authorization: String): Response<BaseResponse<TokenDto>>

}