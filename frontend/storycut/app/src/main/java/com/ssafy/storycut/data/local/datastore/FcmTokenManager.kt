package com.ssafy.storycut.data.local.datastore


import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(private val context: Context) {

    private val PREFS_NAME = "fcm_prefs"
    private val KEY_FCM_TOKEN = "fcm_token"

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getToken(): String {
        return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
    }
}