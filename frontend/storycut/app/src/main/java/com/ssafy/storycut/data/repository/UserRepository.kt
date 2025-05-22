package com.ssafy.storycut.data.repository

import android.util.Log
import com.ssafy.storycut.data.api.model.UpdateUserRequest
import com.ssafy.storycut.data.api.model.credential.UserInfo
import com.ssafy.storycut.data.api.service.AuthApiService
import com.ssafy.storycut.data.local.dao.UserDao
import com.ssafy.storycut.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 정보를 관리하는 리포지토리
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val authApiService: AuthApiService
) {
    /**
     * 사용자 정보를 Room 데이터베이스에 저장
     */
    suspend fun saveUser(userInfo: UserInfo) {
        val userEntity = UserEntity(
            email = userInfo.email,
            name = userInfo.name,
            nickname = userInfo.nickname,
            profileImg = userInfo.profileImg,
            createdAt = userInfo.createdAt,
            updatedAt = userInfo.updatedAt
        )
        userDao.insertUser(userEntity)
    }
    
    /**
     * 현재 로그인된 사용자 정보를 Flow로 가져오기
     */
    fun getCurrentUser(): Flow<UserInfo?> {
        return userDao.getLatestUser().map { entity ->
            entity?.let {
                UserInfo(
                    email = it.email,
                    name = it.name,
                    nickname = it.nickname,
                    profileImg = it.profileImg,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }
    
    /**
     * 특정 이메일의 사용자 정보 가져오기
     */
    fun getUserByEmail(email: String): Flow<UserInfo?> {
        return userDao.getUserByEmail(email).map { entity ->
            entity?.let {
                UserInfo(
                    email = it.email,
                    name = it.name,
                    nickname = it.nickname,
                    profileImg = it.profileImg,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }
    
    /**
     * 로그아웃 - 모든 사용자 정보 삭제 , 서버/로컬 모두
     */
    suspend fun logout() {
        try {
            Log.d("AuthRepository", "시작")
            val response = authApiService.logout()
            Log.d("AuthRepository", "받아옴 : ${response.isSuccessful}")
            if (response.isSuccessful) {
                userDao.deleteAllUsers()
            } else {
                // 서버 로그아웃 실패, 하지만 로컬 데이터는 삭제
                userDao.deleteAllUsers()
                throw Exception("서버 로그아웃 실패: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            // 실패시 로컬 데이터 삭제
            userDao.deleteAllUsers()
            throw e
        }
    }
    // 로컬 삭제
    suspend fun localLogout() {
        userDao.deleteAllUsers()
    }

    /**
     * 특정 ID의 사용자 정보를 API로 조회하기
     */
    suspend fun getMemberById(memberId: Long): Result<UserInfo> {
        return try {
            val response = authApiService.getMemberById(memberId)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(response.body()?.result!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "API 조회 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val response = authApiService.deleteId()
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                // 서버에서 회원 정보 삭제 성공, 로컬 데이터도 삭제
                userDao.deleteAllUsers()
                Result.success(Unit)
            } else {
                // 서버에서 삭제 실패, 에러 반환
                Result.failure(Exception(response.body()?.message ?: "회원 탈퇴 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            // 네트워크 오류 등이 발생했을 때 에러 반환
            Result.failure(Exception("회원 탈퇴 요청 중 오류 발생: ${e.message}", e))
        }
    }

    suspend fun updateUserInfo(nickname: String): Result<UserInfo> {
        return try {
            val updateRequest = UpdateUserRequest(nickname = nickname)
            val response = authApiService.updateMemberDetail(updateRequest)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                // 서버에서 업데이트 성공, 반환된 사용자 정보로 로컬 데이터도 업데이트
                val updatedUserInfo = response.body()?.result!!
                saveUser(updatedUserInfo)
                Result.success(updatedUserInfo)
            } else {
                // 서버에서 업데이트 실패, 에러 반환
                Result.failure(Exception(response.body()?.message ?: "회원 정보 수정 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            // 네트워크 오류 등이 발생했을 때 에러 반환
            Result.failure(Exception("회원 정보 수정 요청 중 오류 발생: ${e.message}", e))
        }
    }

}