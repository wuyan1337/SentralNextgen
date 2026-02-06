package com.sentral.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentral.app.data.CookieManager as AppCookieManager
import com.sentral.app.data.SentralApi
import com.sentral.app.ui.res.LocalAppStrings
import kotlinx.coroutines.launch

/**
 * Cookie 登录界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    cookieManager: AppCookieManager,
    sentralApi: SentralApi,
    settingsManager: com.sentral.app.data.SettingsManager,
    onLoginSuccess: (sessionId: String, studentId: String) -> Unit
) {
    // UI State
    var showWebView by remember { mutableStateOf(false) }
    var showManualLogin by remember { mutableStateOf(false) }
    
    // Auto Login Credentials
    var storedUsername by remember { mutableStateOf(settingsManager.getUsername() ?: "") }
    var storedPassword by remember { mutableStateOf(settingsManager.getPassword() ?: "") }
    var autoLoginEnabled by remember { mutableStateOf(storedUsername.isNotEmpty() && storedPassword.isNotEmpty()) }
    var showPassword by remember { mutableStateOf(false) }

    // Manual Login State
    var cookie by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var showCookie by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current

    if (showWebView) {
        LoginWebView(
            username = if (autoLoginEnabled) storedUsername else null,
            password = if (autoLoginEnabled) storedPassword else null,
            onLoginSuccess = { cookies, capturedSessionId ->
                scope.launch {
                    // 1. Save Cookie (Critical)
                    cookieManager.saveCookie(cookies)
                    
                    // 2. Save Session ID
                    if (capturedSessionId != "auto") {
                        cookieManager.saveSessionId(capturedSessionId)
                    }
                    
                    // 3. Save Dummy Student ID
                    if (cookieManager.getStudentId() == null) {
                        cookieManager.saveStudentId("auto_refresh_needed")
                    }

                    // 4. Update UI / State
                    showWebView = false
                    isChecking = true
                    
                    try {
                        val finalSessionId = if (capturedSessionId != "auto") capturedSessionId else "auto"
                        onLoginSuccess(finalSessionId, "auto") 
                    } catch (e: Exception) {
                        errorMessage = "Login setup failed: ${e.message}"
                        isChecking = false
                    }
                }
            },
            onLoginFailed = { reason ->
                // Handle Login Failure
                showWebView = false
                
                // Map raw website error to localized string
                val localizedReason = when {
                    reason.contains("password", ignoreCase = true) || 
                    reason.contains("username", ignoreCase = true) ||
                    reason.contains("credential", ignoreCase = true) ||
                    reason.contains("密码") || 
                    reason.contains("用户名") ||
                    reason.contains("不正确") -> strings.invalidCredentials
                    else -> reason
                }

                val formattedReason = try {
                    String.format(strings.loginFailed, localizedReason)
                } catch (e: Exception) {
                    "Login Failed: $localizedReason"
                }
                errorMessage = formattedReason
                // Disable auto-login so user can try again
                autoLoginEnabled = false
            },
            onCancel = { showWebView = false }
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appName) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon
            Text(text = "🔐", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strings.welcomeBack,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.loginSubtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            

            
            // Primary Action: WebView Login
            Button(
                onClick = { showWebView = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(if (autoLoginEnabled) strings.autoLoginBtn else strings.loginButton, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error Message
            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Auto Login Setup Toggle
            var showAutoLoginSetup by remember { mutableStateOf(false) }
            var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
            
            TextButton(
                onClick = { showAutoLoginSetup = !showAutoLoginSetup },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showAutoLoginSetup) strings.hideAutoLogin else strings.setupAutoLogin)
            }
            
            if (showAutoLoginSetup) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.autoLoginTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.autoLoginDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = storedUsername,
                            onValueChange = { 
                                storedUsername = it 
                                saveSuccessMessage = null // Reset message on edit
                            },
                            label = { Text(strings.usernameHint) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = storedPassword,
                            onValueChange = { 
                                storedPassword = it
                                saveSuccessMessage = null
                            },
                            label = { Text(strings.password) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Text(if (showPassword) "🔒" else "👁️")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                settingsManager.saveCredentials(storedUsername, storedPassword)
                                autoLoginEnabled = true
                                saveSuccessMessage = strings.saveSuccess
                            },
                            enabled = storedUsername.isNotBlank() && storedPassword.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.saveCredentials)
                        }
                        
                        saveSuccessMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Advanced Toggle
            TextButton(onClick = { showManualLogin = !showManualLogin }) {
                Text(if (showManualLogin) strings.hideAdvanced else strings.advancedOptions)
            }
            
            if (showManualLogin) {
                // Manual Manual Fields
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Sentral URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isChecking
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    label = { Text(strings.cookieHint) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4,
                    visualTransformation = if (showCookie) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCookie = !showCookie }) {
                             Text(if (showCookie) "🔒" else "👁️")
                        }
                    },
                    enabled = !isChecking
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            errorMessage = null
                            if (cookie.isNotBlank() && url.isNotBlank()) {
                                // Manual parsing logic
                                val sessionIdMatch = Regex("/s-([a-zA-Z0-9]+)/").find(url)
                                val studentIdMatch = Regex("/student/(\\d+)").find(url)
                                val sessionId = sessionIdMatch?.groupValues?.get(1)
                                val studentId = studentIdMatch?.groupValues?.get(1)
                                if (sessionId != null && studentId != null) {
                                    cookieManager.saveCookie(cookie.trim())
                                    onLoginSuccess(sessionId, studentId)
                                } else {
                                    errorMessage = "Could not parse IDs from URL."
                                }
                            } else {
                                errorMessage = "Please fill all fields."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking
                ) {
                   Text(strings.manualLoginBtn) 
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
             // Contact Author
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            TextButton(
                onClick = { uriHandler.openUri("https://t.me/wuyan1337") }
            ) {
                Text(
                    text = strings.contactAuthor,
                    color = MaterialTheme.colorScheme.primary.copy(alpha=0.5f),
                    fontSize = 12.sp
                )
            }
            
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/wuyan1337/SentralNextgen") }
            ) {
                Text(
                    text = strings.githubLink,
                    color = MaterialTheme.colorScheme.primary.copy(alpha=0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

