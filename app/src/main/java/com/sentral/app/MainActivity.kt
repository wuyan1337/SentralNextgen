package com.sentral.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.sentral.app.data.CookieManager
import com.sentral.app.data.SentralApi
import com.sentral.app.ui.LoginScreen
import com.sentral.app.ui.TimetableScreen
import com.sentral.app.ui.theme.SentralTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var cookieManager: CookieManager
    private lateinit var sentralApi: SentralApi
    private lateinit var settingsManager: com.sentral.app.data.SettingsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        cookieManager = CookieManager(this)
        sentralApi = SentralApi(cookieManager)
        settingsManager = com.sentral.app.data.SettingsManager(this)
        
        setContent {
            val isDarkTheme by settingsManager.isDarkTheme.collectAsState()
            val language by settingsManager.language.collectAsState()
            
            val appStrings = if (language == "zh") com.sentral.app.ui.res.ChineseStrings else com.sentral.app.ui.res.EnglishStrings
            
            CompositionLocalProvider(com.sentral.app.ui.res.LocalAppStrings provides appStrings) {
                SentralTheme(darkTheme = isDarkTheme) {
                    var isLoggedIn by remember { mutableStateOf(cookieManager.isLoggedIn()) }
                    
                    if (isLoggedIn) {
                        TimetableScreen(
                            sentralApi = sentralApi,
                            settingsManager = settingsManager,
                            onLogout = {
                                cookieManager.logout()
                                sentralApi.clearCache()
                                isLoggedIn = false
                            }
                        )
                    } else {
                        LoginScreen(
                            cookieManager = cookieManager,
                            sentralApi = sentralApi,
                            settingsManager = settingsManager,
                            onLoginSuccess = { sessionId, studentId ->
                                // 保存 session 信息
                                cookieManager.saveSessionId(sessionId)
                                if (studentId.isNotEmpty()) {
                                    cookieManager.saveStudentId(studentId)
                                }
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}
