package com.ssafy.storycut.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ssafy.storycut.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 정보에 접근하기 위한 DAO 인터페이스
 */
@Dao
interface UserDao {
    /**
     * 사용자 정보를 삽입
     * 동일한 이메일의 사용자가 있으면 대체
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    /**
     * 모든 사용자 정보 가져오기
     * Flow로 반환하여 변경 사항 관찰 가능
     */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    /**
     * 특정 이메일의 사용자 정보 가져오기
     */
    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<UserEntity?>
    
    /**
     * 가장 최근에 업데이트된 사용자 정보 가져오기
     */
    @Query("SELECT * FROM users ORDER BY updatedAt DESC LIMIT 1")
    fun getLatestUser(): Flow<UserEntity?>
    
    /**
     * 모든 사용자 정보 삭제 (로그아웃 시 사용)
     */
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}