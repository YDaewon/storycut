package com.ssafy.storycut.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자 정보를 저장하는 Entity 클래스
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val email: String,
    val name: String,
    val nickname: String,
    val profileImg: String,
    val createdAt: String,
    val updatedAt: String
)