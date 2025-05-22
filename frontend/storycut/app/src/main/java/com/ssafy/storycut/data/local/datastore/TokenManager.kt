package com.ssafy.storycut.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Context 확장 프로퍼티는 파일의 최상위 레벨에 있어야 합니다
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val GOOGLE_ACCESS_TOKEN = stringPreferencesKey("google_access_token")
    }

    // 토큰 저장
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveGoogleAccessTokens(googleAccessToken: String) {
        context.dataStore.edit { preferences ->
            preferences[GOOGLE_ACCESS_TOKEN] = googleAccessToken
        }
    }

    // 액세스 토큰 조회
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }

    // 리프레시 토큰 조회
    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }

    // 구글 액세스 토큰 조회
    val googleAccessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GOOGLE_ACCESS_TOKEN]
    }

    // 토큰 삭제 (로그아웃)
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
        }
    }
}