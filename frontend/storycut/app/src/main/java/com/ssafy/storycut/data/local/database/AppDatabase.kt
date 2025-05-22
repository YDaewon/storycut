package com.ssafy.storycut.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ssafy.storycut.data.local.dao.UserDao
import com.ssafy.storycut.data.local.entity.UserEntity

/**
 * 앱의 Room 데이터베이스
 */
@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 사용자 정보 DAO 제공
     */
    abstract fun userDao(): UserDao
    
    companion object {
        // 싱글톤 인스턴스
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 데이터베이스 인스턴스 가져오기
         * 없으면 생성, 있으면 기존 인스턴스 반환
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "storycut_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}