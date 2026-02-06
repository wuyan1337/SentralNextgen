package com.sentral.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sentral.app.ui.res.LocalAppStrings

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebView(
    username: String? = null,
    password: String? = null,
    onLoginSuccess: (cookies: String, sessionId: String) -> Unit,
    onLoginFailed: (reason: String) -> Unit,
    onCancel: () -> Unit
) {
    // JS Injection Scripts
    val autoLoginScript = """
        (function() {
            if (window.sentralAutoLoginRunning) return; 
            window.sentralAutoLoginRunning = true;
            
            function setValue(element, value) {
                element.value = value;
                element.dispatchEvent(new Event('input', { bubbles: true }));
                element.dispatchEvent(new Event('change', { bubbles: true }));
            }

            var checkInterval = setInterval(function() {
                
                // 1. Portal Selection Page (Sentral)
                var studentBtns = document.querySelectorAll('button');
                for (var i = 0; i < studentBtns.length; i++) {
                    if (studentBtns[i].outerHTML.indexOf('login_student') !== -1 || studentBtns[i].innerText.indexOf('Student') !== -1) {
                        if (studentBtns[i].offsetParent !== null) {
                            studentBtns[i].click();
                            return; 
                        }
                    }
                }
                
                // 2. Identity Server (Microsoft / DET)
                
                // --- USERNAME ---
                var emailInput = document.querySelector('input[name="loginfmt"]') || document.querySelector('input[type="email"]');
                var nextBtn = document.getElementById('idSIButton9') || document.querySelector('input[type="submit"]') || document.getElementById('DSButtonID');
                var targetUser = "$username";
                
                if (targetUser && targetUser !== "null" && emailInput) {
                   if (targetUser.indexOf('@') === -1) targetUser += "@education.nsw.gov.au";

                   if (emailInput.value !== targetUser) {
                        setValue(emailInput, targetUser);
                        if (nextBtn) {
                             setTimeout(function() { nextBtn.click(); }, 500);
                        }
                        return;
                   } else {
                        if (nextBtn && nextBtn.offsetParent !== null && !nextBtn.disabled) {
                            nextBtn.click();
                            return;
                        }
                   }
                }
                
                // --- PASSWORD ---
                var pwdInput = document.querySelector('input[name="passwd"]') || document.querySelector('input[type="password"]');
                var submitBtn = document.getElementById('idSIButton9') || document.querySelector('input[type="submit"]') || document.getElementById('submitButton');
                var targetPwd = "$password";
                
                if (targetPwd && targetPwd !== "null" && pwdInput) {
                    if (pwdInput.value === "") {
                        setValue(pwdInput, targetPwd);
                        
                        var kmsi = document.querySelector('input[name="KeepMeSignedIn"]') || document.getElementById('KmsiCheckboxField');
                        if (kmsi && !kmsi.checked) kmsi.click();
                        
                        if (submitBtn) {
                            setTimeout(function() { submitBtn.click(); }, 500);
                        }
                        return;
                    }
                }
                
                // --- ERROR DETECTION ---
                // 1. Specific Identity Server Error
                var errorTextSpan = document.getElementById('errorText');
                if (errorTextSpan && errorTextSpan.innerText.length > 0 && window.sentralLastErr !== errorTextSpan.innerText) {
                     window.sentralLastErr = errorTextSpan.innerText;
                     if (window.SentralAndroid) {
                         window.SentralAndroid.onLoginError(errorTextSpan.innerText);
                     }
                     return;
                }

                // 2. Microsoft Standard Errors
                var errDiv = document.getElementById('usernameError') || document.getElementById('passwordError');
                if (errDiv && errDiv.innerText.length > 0 && window.sentralLastErr !== errDiv.innerText) {
                     window.sentralLastErr = errDiv.innerText;
                     if (window.SentralAndroid) {
                         window.SentralAndroid.onLoginError(errDiv.innerText);
                     }
                     return;
                }
                
                // 3. Sentral Generic Errors
                var sentralErr = document.querySelector('div.alert-danger') || document.querySelector('div.error');
                if (sentralErr && sentralErr.innerText.includes('Invalid') && window.sentralLastErr !== sentralErr.innerText) {
                     window.sentralLastErr = sentralErr.innerText;
                     if (window.SentralAndroid) {
                         window.SentralAndroid.onLoginError(sentralErr.innerText);
                     }
                     return;
                }

                // --- STAY SIGNED IN PROMPT ---
                var title = document.querySelector('.text-title');
                if (title && (title.innerText.includes('Stay signed in') || title.innerText.includes('保持登录'))) {
                     var yesBtn = document.getElementById('idSIButton9') || document.querySelector('input[type="submit"]');
                     if (yesBtn) yesBtn.click();
                }

            }, 800);
        })();
    """.trimIndent()

    val isHeadless = !username.isNullOrEmpty() && !password.isNullOrEmpty()
    
    // Web Interface for JS callbacks
    class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun onLoginError(reason: String) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onLoginFailed(reason)
            }
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isHeadless) 0.01f else 1f),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        
                        addJavascriptInterface(WebAppInterface(), "SentralAndroid")
                        
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()

                        val sessionRegex = Regex("/s-([a-zA-Z0-9]+)/")
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                                    view?.evaluateJavascript(autoLoginScript, null)
                                }
                                url?.let { currentUrl ->
                                    if (currentUrl.contains("/portal/dashboard") || currentUrl.contains("/portal/main") || currentUrl.contains("/s-")) {
                                        val cookieManager = CookieManager.getInstance()
                                        val cookies = cookieManager.getCookie(currentUrl)
                                        val sessionMatch = sessionRegex.find(currentUrl)
                                        val sessionId = sessionMatch?.groupValues?.get(1)
                                        
                                        if (cookies != null && sessionId != null) {
                                            onLoginSuccess(cookies, sessionId)
                                        } else if (cookies != null && currentUrl.contains("sentral.com.au") && !currentUrl.contains("login")) {
                                            onLoginSuccess(cookies, "auto")
                                        }
                                    }
                                }
                            }
                        }
                        loadUrl("https://castlehill-h.sentral.com.au/auth/portal")
                    }
                },
                update = { }
            )
            
            // Loading Overlay
            if (isHeadless) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val strings = LocalAppStrings.current
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(scale)
                                .alpha(0.1f)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = strings.autoLoginStatus,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.autoLoginWait,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
