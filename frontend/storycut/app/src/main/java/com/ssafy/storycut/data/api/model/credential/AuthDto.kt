package com.ssafy.storycut.data.api.model.credential


/**
 * 사용자 정보 모델
 */
data class UserInfo(
    val email: String,
    val name: String,
    val nickname: String,
    val profileImg: String,
    val createdAt: String,
    val updatedAt: String
)