package com.sentral.app.ui.res

import androidx.compose.runtime.staticCompositionLocalOf

data class AppStrings(
    // Global
    val appName: String,
    val settings: String,
    
    // Login
    val welcomeBack: String,
    val loginSubtitle: String,
    val cookieHint: String,
    val loginButton: String,
    val howToGetCookie: String,
    val loggingIn: String,
    
    // Settings
    val appearance: String,
    val language: String,
    val lightMode: String,
    val darkMode: String,
    val english: String,
    val chinese: String,
    val logout: String,
    val logoutConfirm: String,
    
    // Timetable
    val timetable: String, // "Timetable" or "课表"
    val today: String,
    val tomorrow: String,
    val refresh: String,
    val noLessons: String,
    
    // Auto Login & Privacy (New)
    val autoLoginSetup: String,
    val hideAutoLogin: String,
    val setupAutoLogin: String,
    val autoLoginTitle: String,
    val autoLoginDesc: String,
    val username: String,
    val usernameHint: String,
    val password: String,
    val saveCredentials: String,
    val saveSuccess: String,
    val autoLoginBtn: String,
    val manualLoginBtn: String,
    val advancedOptions: String,
    val hideAdvanced: String,
    val contactAuthor: String,
    val githubLink: String,
    val privacyTitle: String,
    val privacyContent: String, // Multi-line content
    val privacyAccept: String,
    val privacyWait: String,
    
    // Notifications & Tasks
    val notifications: String,
    val classAlerts: String,
    val classAlertsDesc: String,
    val offlineMode: String,
    val welcomeUser: String,
    val checkTask: String,
    val noteTitle: String,
    val noteHint: String,
    val save: String,
    val cancel: String,
    val happeningNow: String,
    val now: String,
    val autoLoginStatus: String,
    val autoLoginWait: String,
    val loginFailed: String,
    
    // Widget
    val widgetNoData: String,
    val widgetOpenApp: String,
    val widgetAllDone: String,
    val widgetNoMoreClasses: String,
    val widgetKeepItUp: String,
    val widgetNow: String,
    val widgetNext: String,
    
    // Errors
    val invalidCredentials: String,
    val unknownError: String
)

val EnglishStrings = AppStrings(
    appName = "Sentral Nextgen",
    settings = "Settings",
    welcomeBack = "Welcome Back",
    loginSubtitle = "Secure Login via School Portal",
    cookieHint = "Paste your cookie here...",
    loginButton = "Sign In",
    howToGetCookie = "How to get cookie?",
    loggingIn = "Signing in...",
    appearance = "Appearance",
    language = "Language",
    lightMode = "Light",
    darkMode = "Dark",
    english = "English",
    chinese = "Chinese",
    logout = "Log Out",
    logoutConfirm = "Are you sure you want to log out?",
    timetable = "Timetable",
    today = "Today",
    tomorrow = "Tomorrow",
    refresh = "Refresh",
    noLessons = "No Lesson",
    
    // New
    autoLoginSetup = "Auto-Login Setup",
    hideAutoLogin = "Hide Auto-Login Setup",
    setupAutoLogin = "Setup Auto-Login (Optional)",
    autoLoginTitle = "Auto-Login Credentials",
    autoLoginDesc = "Enter your school credentials to enable one-click login.",
    username = "Username",
    usernameHint = "Username",
    password = "Password",
    saveCredentials = "Save Credentials",
    saveSuccess = "Credentials Saved Successfully! ✓",
    autoLoginBtn = "Auto Log in with Dashboard",
    manualLoginBtn = "Manual Login",
    advancedOptions = "Advanced Options",
    hideAdvanced = "Hide Advanced Options",
    contactAuthor = "Contact Author @wuyan1337",
    githubLink = "Github",
    privacyTitle = "Privacy & Usage Policy",
    privacyContent = """
        1. We do not collect ANY personal information. All credentials and cookies are stored locally on your device.
        2. This app is an unofficial client and is not affiliated with Sentral or NSW Education.
        3. Usage of this app is at your own risk. The developer is not responsible for any issues arising from its use.
        4. We do not track your usage, location, or any other data.
        5. This app communicates directly with Sentral servers only.
    """.trimIndent(),
    privacyAccept = "I Understand and Accept",
    privacyWait = "Please read for %ds",
    
    // Notifications & Tasks
    notifications = "Notifications",
    classAlerts = "Class Alerts",
    classAlertsDesc = "5 mins before class",
    offlineMode = "Offline Mode - Showing Cached Data",
    welcomeUser = "Welcome, %s",
    checkTask = "Homework / To-Do",
    noteTitle = "Note: %s",
    noteHint = "Enter homework or notes...",
    save = "Save",
    cancel = "Cancel",
    happeningNow = "HAPPENING NOW",
    now = "NOW",
    autoLoginStatus = "Auto Logging in...",
    autoLoginWait = "Please wait while we access your school portal",
    loginFailed = "Login Failed: %s",
    
    // Widget
    widgetNoData = "No Data",
    widgetOpenApp = "Open App to Sync",
    widgetAllDone = "All Done",
    widgetNoMoreClasses = "No more classes today",
    widgetKeepItUp = "Keep it up!",
    widgetNow = "NOW",
    widgetNext = "NEXT UP",
    
    // Errors
    invalidCredentials = "Invalid username or password.",
    unknownError = "Unknown error occurred."
)

val ChineseStrings = AppStrings(
    appName = "Sentral Nextgen",
    settings = "设置",
    welcomeBack = "欢迎回来",
    loginSubtitle = "通过学校官网安全登录",
    cookieHint = "在此粘贴 Cookie...",
    loginButton = "登录",
    howToGetCookie = "如何获取 Cookie?",
    loggingIn = "登录中...",
    appearance = "外观",
    language = "语言",
    lightMode = "亮色",
    darkMode = "深色",
    english = "English",
    chinese = "中文",
    logout = "退出登录",
    logoutConfirm = "确定要退出登录吗？",
    timetable = "课表",
    today = "今日",
    tomorrow = "明日",
    refresh = "刷新",
    noLessons = "无课程",
    
    // New
    autoLoginSetup = "自动登录设置",
    hideAutoLogin = "隐藏自动登录设置",
    setupAutoLogin = "设置自动登录 (可选)",
    autoLoginTitle = "自动登录凭证",
    autoLoginDesc = "输入您的学校账号以启用一键登录功能。",
    username = "用户名",
    usernameHint = "用户名",
    password = "密码",
    saveCredentials = "保存凭证",
    saveSuccess = "凭证保存成功！✓",
    autoLoginBtn = "自动登录并进入",
    manualLoginBtn = "手动登录",
    advancedOptions = "高级选项",
    hideAdvanced = "隐藏高级选项",
    contactAuthor = "联系作者 @wuyan1337",
    githubLink = "Github",
    privacyTitle = "隐私与使用协议",
    privacyContent = """
        1. 我们承诺不收集您的任何个人信息。所有的账号、密码和 Cookie 均仅存储在您的设备本地。
        2. 本应用为非官方客户端，与 Sentral 或 NSW Education 无任何关联。
        3. 使用本应用产生的任何风险由您自行承担，开发者不对使用过程中产生的问题负责。
        4. 我们不会追踪您的使用习惯、位置或其他任何数据。
        5. 本应用仅直接与 Sentral 官方服务器进行通信。
    """.trimIndent(),
    privacyAccept = "我已阅读并接受",
    privacyWait = "请阅读 %d 秒",
    
    // Notifications & Tasks
    notifications = "通知",
    classAlerts = "上课提醒",
    classAlertsDesc = "每节课前5分钟提醒",
    offlineMode = "离线模式 - 显示缓存数据",
    welcomeUser = "欢迎, %s",
    checkTask = "作业 / 待办",
    noteTitle = "笔记: %s",
    noteHint = "输入作业或笔记...",
    save = "保存",
    cancel = "取消",
    happeningNow = "正在上课",
    now = "当前",
    autoLoginStatus = "自动登录中...",
    autoLoginWait = "请稍候，正在连接学校官网...",
    loginFailed = "登录失败: %s",
    
    // Widget
    widgetNoData = "无数据",
    widgetOpenApp = "请打开应用同步",
    widgetAllDone = "课程结束",
    widgetNoMoreClasses = "今天没有更多课程了",
    widgetKeepItUp = "继续加油!",
    widgetNow = "当前课程",
    widgetNext = "下节预告",
    
    // Errors
    invalidCredentials = "用户名或密码错误。",
    unknownError = "发生未知错误。"
)

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }

// Helper for non-Composable contexts
fun getAppStrings(context: android.content.Context): AppStrings {
    val prefs = context.getSharedPreferences("app_settings_v2", android.content.Context.MODE_PRIVATE)
    val lang = prefs.getString("language", "en") ?: "en"
    return if (lang == "zh") ChineseStrings else EnglishStrings
}
