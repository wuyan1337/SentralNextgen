package com.sentral.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Cookie 管理器 - 持久化存储 Sentral Cookie
 */
class CookieManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "sentral_prefs"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_STUDENT_ID = "student_id"
    }
    
    /**
     * 保存 Cookie
     */
    fun saveCookie(cookie: String) {
        prefs.edit().putString(KEY_COOKIE, cookie).apply()
    }
    
    /**
     * 获取保存的 Cookie
     */
    fun getCookie(): String? {
        return prefs.getString(KEY_COOKIE, null)
    }
    
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        val cookie = getCookie()
        val sessionId = getSessionId()
        return !cookie.isNullOrEmpty() && cookie.contains("PortalSID2") && !sessionId.isNullOrEmpty()
    }
    
    /**
     * 获取 Session ID
     */
    fun getSessionId(): String? {
        return prefs.getString(KEY_SESSION_ID, null)
    }
    
    /**
     * 保存 Session ID
     */
    fun saveSessionId(sessionId: String) {
        prefs.edit().putString(KEY_SESSION_ID, sessionId).apply()
    }
    
    /**
     * 获取 Student ID
     */
    fun getStudentId(): String? {
        return prefs.getString(KEY_STUDENT_ID, null)
    }
    
    /**
     * 保存 Student ID
     */
    fun saveStudentId(studentId: String) {
        prefs.edit().putString(KEY_STUDENT_ID, studentId).apply()
    }
    
    /**
     * 清除登录状态
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
