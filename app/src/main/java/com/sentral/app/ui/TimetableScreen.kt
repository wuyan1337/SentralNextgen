package com.sentral.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.sentral.app.data.SentralApi
import com.sentral.app.data.SettingsManager
import com.sentral.app.model.TimetableEntry
import com.sentral.app.ui.res.LocalAppStrings
import com.sentral.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    sentralApi: SentralApi,
    settingsManager: SettingsManager,
    onLogout: () -> Unit
) {
    var timetable by remember { mutableStateOf<List<TimetableEntry>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Today, 1 for Tomorrow
    
    // Settings State
    var showSettings by remember { mutableStateOf(false) }
    
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    
    // Managers - Fix for implicit context in remember
    val context = androidx.compose.ui.platform.LocalContext.current
    val cacheManager = remember { com.sentral.app.data.CacheManager(context) }
    val taskManager = remember { com.sentral.app.data.TaskManager(context) }
    val notificationManager = remember { com.sentral.app.data.SentralNotificationManager(context) }
    
    var isOffline by remember { mutableStateOf(false) }

    // Task Dialog State
    var showTaskDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("") }
    var currentTaskContent by remember { mutableStateOf("") }
    var taskUpdateTrigger by remember { mutableIntStateOf(0) }
    
    // Permissions (Android 13+)
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {}
    
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val loadTimetable = {
        scope.launch {
            isLoading = true
            error = null
            isOffline = false
            
            val calendar = Calendar.getInstance()
            if (selectedTab == 1) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            try {
                // 1. Try Network
                val result = sentralApi.getTimetable(calendar.time)
                
                if (result != null) {
                    timetable = result
                    // Process Success Actions
                    if (selectedTab == 0) { 
                         cacheManager.saveTimetable(result)
                         
                         // Schedule Notifications only if enabled
                         // Note: We need to access the flow value directly or collect it. 
                         // Since we are in a coroutine, we can access .value if StateFlow or simply passed in.
                         // But better to observe it outside. Ideally we pass 'notificationsEnabled' to loadTimetable 
                         // or check it here. For simplicity, let's assume we check the repo/manager.
                         // However, 'settingsManager' is available.
                         if (settingsManager.notificationsEnabled.value) {
                             notificationManager.scheduleNotifications(result)
                         }
                    }
                } else {
                    throw Exception("No data from server")
                }
            } catch (e: Exception) {
                // 2. Network Failed -> Try Cache
                val cached = cacheManager.getTimetable()
                if (cached != null && cached.isNotEmpty()) {
                    timetable = cached
                    isOffline = true
                } else {
                     error = "无法连接且无缓存: ${e.message}"
                }
            }
            isLoading = false
        }
    }
    
    // 首次加载和切换 Tab 时重新加载
    LaunchedEffect(selectedTab) {
        loadTimetable()
    }
    
    // 获取用户名字
    var userName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val name = sentralApi.getUserInfo()
        if (name != null) {
            userName = name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (userName.isNotEmpty()) String.format(strings.welcomeUser, userName) else strings.timetable,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background, // Clean background
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { loadTimetable() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = strings.refresh
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = strings.settings
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.today) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.tomorrow) }
                )
            }
            
            // Date Header
            val calendar = Calendar.getInstance()
            if (selectedTab == 1) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            // Observe language for date formatting
            val currentLanguage by settingsManager.language.collectAsState()
            val locale = if (currentLanguage == "en") Locale.ENGLISH else Locale.CHINA
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd EEEE", locale)
            val dateStr = dateFormat.format(calendar.time)
            
            Text(
                text = dateStr,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            if (isOffline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = strings.offlineMode,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (error != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { loadTimetable() }) {
                            Text(strings.refresh)
                        }
                    }
                } else if (timetable.isNullOrEmpty()) {
                    Text(
                        text = strings.noLessons,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(timetable!!) { entry ->
                             // Check if this class is happening now
                            val isCurrent = if (selectedTab == 0) isTimeNow(entry.timeStart, entry.timeEnd) else false
                            // Check Task
                            val hasTask = taskUpdateTrigger.let { taskManager.hasTask(entry.subject) }
                            
                            TimetableCard(
                                entry = entry, 
                                isCurrent = isCurrent,
                                hasTask = hasTask,
                                onClick = {
                                    if (!entry.isFree) {
                                        selectedSubject = entry.subject
                                        currentTaskContent = taskManager.getTask(entry.subject) ?: ""
                                        showTaskDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showSettings) {
        SettingsDialog(
            settingsManager = settingsManager,
            onDismiss = { showSettings = false },
            onLogout = {
                showSettings = false
                onLogout()
            }
        )
    }
    
    if (showTaskDialog) {
        TaskDialog(
            subject = selectedSubject,
            initialContent = currentTaskContent,
            onDismiss = { showTaskDialog = false },
            onSave = { content ->
                taskManager.saveTask(selectedSubject, content)
                taskUpdateTrigger++ 
                showTaskDialog = false
            }
        )
    }
}

// Helper to check if current time is within range
fun isTimeNow(start: String, end: String): Boolean {
    try {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute
        
        fun parseTime(timeStr: String): Int {
            val parts = timeStr.split(":")
            return parts[0].toInt() * 60 + parts[1].toInt()
        }
        
        val startTime = parseTime(start)
        val endTime = parseTime(end)
        
        return currentTime in startTime until endTime
    } catch (e: Exception) {
        return false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableCard(
    entry: TimetableEntry, 
    isCurrent: Boolean = false, 
    hasTask: Boolean = false,
    onClick: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    
    val periodStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
    
    val borderStroke = if (isCurrent) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }
    
    if (entry.isFree) {
         Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 2.dp),
            border = borderStroke
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                 Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                )
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.width(60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = entry.period,
                            style = periodStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.timeStart,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                         Text(
                            text = entry.timeEnd,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = strings.noLessons, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                         if (isCurrent) {
                            Text(
                                text = strings.now,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
         }
         return
    }

    val accentColor = try {
        Color(android.graphics.Color.parseColor(entry.bgColor.ifEmpty { "#007AFF" }))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    val containerColor = accentColor.copy(alpha = 0.08f)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor, 
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 0.dp),
        border = borderStroke
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (isCurrent) MaterialTheme.colorScheme.primary else accentColor)
            )
            
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(55.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = entry.period,
                        style = periodStyle, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.timeStart,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = entry.timeEnd,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    if (isCurrent) {
                         Text(
                            text = "HAPPENING NOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = entry.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (entry.teacher.isNotEmpty()) {
                        Text(
                            text = entry.teacher,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    if (entry.className.isNotEmpty()) {
                         Text(
                            text = entry.className,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (entry.room.isNotEmpty()) {
                        Surface(
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = entry.room,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    if (hasTask) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Has Task",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun TaskDialog(
    subject: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialContent) }
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = String.format(strings.noteTitle, subject)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(strings.noteHint) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
