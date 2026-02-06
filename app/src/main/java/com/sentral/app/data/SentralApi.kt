package com.sentral.app.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sentral.app.model.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Sentral API 客户端
 */
class SentralApi(
    private val cookieManager: CookieManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val baseUrl = "https://castlehill-h.sentral.com.au"
    
    private var cachedTimetable: JsonArray? = null
    
    /**
     * 获取完整课表数据 (通过 getFullTimetableInDates)
     */
    suspend fun getFullTimetable(): JsonArray? = withContext(Dispatchers.IO) {
        val cookie = cookieManager.getCookie() ?: return@withContext null
        val sessionId = cookieManager.getSessionId() ?: return@withContext null
        var studentId = cookieManager.getStudentId() ?: return@withContext null
        
        // Auto-resolve Student ID if it's a placeholder
        if (studentId == "auto" || studentId == "auto_refresh_needed" || studentId.isEmpty()) {
            val resolvedId = fetchStudentId(sessionId)
            if (resolvedId != null) {
                studentId = resolvedId
                cookieManager.saveStudentId(resolvedId)
            } else {
                 return@withContext null
            }
        }
        
        // 使用新的 API 端点
        val url = "$baseUrl/s-$sessionId/portal/timetable/getFullTimetableInDates/$studentId/undefined/true"
        
        try {
            val request = Request.Builder()
                .url(url)
                .header("Cookie", cookie)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                cachedTimetable = gson.fromJson(body, JsonArray::class.java)
                cachedTimetable
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resolves the Student ID by querying /portal/user
     */
    private fun fetchStudentId(sessionId: String): String? {
        val url = "$baseUrl/s-$sessionId/portal/user"
        try {
            // Logic copied from getUserInfo: POST first, then GET
            val bodyJson = "{\"action\":\"is_authenticated\"}"
            val requestBody = bodyJson.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Cookie", cookieManager.getCookie() ?: "")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Referer", "$baseUrl/s-$sessionId/portal/")
                .header("Origin", baseUrl)
                .header("Accept", "application/json")
                .build()
             
             var response = client.newCall(request).execute()
             var body = ""
             
             if (response.isSuccessful) {
                 body = response.body?.string() ?: ""
             }
             
             // Fallback to GET if POST failed or empty
             if (!response.isSuccessful || body.isEmpty()) {
                 try {
                     val getRequest = Request.Builder()
                        .url(url)
                        .get()
                        .header("Cookie", cookieManager.getCookie() ?: "")
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                        .header("Accept", "application/json")
                        .build()
                     response = client.newCall(getRequest).execute()
                     if (response.isSuccessful) {
                         body = response.body?.string() ?: ""
                     }
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
             }

             if (!body.isEmpty()) {
                 val json = gson.fromJson(body, JsonObject::class.java)
                 return json.get("student_id")?.asString 
                     ?: json.get("id")?.asString
             }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取用户信息
     */
    suspend fun getUserInfo(): String? = withContext(Dispatchers.IO) {
        val cookie = cookieManager.getCookie() ?: return@withContext null
        val sessionId = cookieManager.getSessionId() ?: return@withContext null
        
        val url = "$baseUrl/s-$sessionId/portal/user"
        
        try {
            // Log shows it's a POST request with content-type json.
            // Payload verified from user screenshot: {"action":"is_authenticated"}
            val bodyJson = "{\"action\":\"is_authenticated\"}"

            val requestBody = bodyJson.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Cookie", cookie)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .header("Referer", "$baseUrl/s-$sessionId/portal/")
                .header("Origin", baseUrl)
                .header("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            var json: JsonObject? = null
            
            if (response.isSuccessful) {
                val respBody = response.body?.string()
                if (!respBody.isNullOrEmpty()) {
                    json = gson.fromJson(respBody, JsonObject::class.java)
                }
            }
            
            // 如果 POST 失败（返回空），尝试 GET
            if (json == null) {
                 try {
                     val getRequest = Request.Builder()
                        .url(url)
                        .get()
                        .header("Cookie", cookie)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                        .header("Accept", "application/json")
                        .build()
                     
                     val getResponse = client.newCall(getRequest).execute()
                     if (getResponse.isSuccessful) {
                         val getBody = getResponse.body?.string()
                         if (!getBody.isNullOrEmpty()) {
                             json = gson.fromJson(getBody, JsonObject::class.java)
                         }
                     }
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
            }

            if (json != null) {
                // 优先使用 first_name，如果不存在则使用 name
                val firstName = if (json.has("first_name")) json.get("first_name")?.asString else null
                if (!firstName.isNullOrEmpty()) {
                    firstName
                } else {
                    if (json.has("name")) json.get("name")?.asString else "Unknown"
                }
            } else {
                "Err: NoData"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Err: ${e.javaClass.simpleName}"
        }
    }
    
    /**
     * 获取指定日期的课表
     */
    suspend fun getTimetable(date: Date = Date()): List<TimetableEntry>? = withContext(Dispatchers.IO) {
        if (cachedTimetable == null) {
            getFullTimetable()
        }
        
        cachedTimetable?.let { data ->
            extractDayTimetable(data, date)
        }
    }
    
    /**
     * 从完整课表中提取指定日期的课程 (新版解析逻辑)
     */
    private fun extractDayTimetable(data: JsonArray, targetDate: Date): List<TimetableEntry> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(targetDate)
        val timetable = mutableListOf<TimetableEntry>()
        
        try {
            for (weekElement in data) {
                val weekData = weekElement.asJsonObject
                // "dates" 是一个 Map (Object)，不是 Array
                val dates = weekData.getAsJsonObject("dates") ?: continue
                
                // 遍历 Map 的值
                for (dateKey in dates.keySet()) {
                    val dayInfo = dates.getAsJsonObject(dateKey) ?: continue
                    
                    // 检查日期是否匹配
                    if (dayInfo.get("date_name")?.asString == dateStr) {
                        
                        // 找到了目标日期，解析 periods 数组
                        val periods = dayInfo.getAsJsonArray("period") ?: continue
                        
                        for (periodElement in periods) {
                            val periodData = periodElement.asJsonObject
                            
                            val periodName = periodData.get("name")?.asString ?: ""
                            val periodStart = periodData.get("start_time")?.asString ?: ""
                            val periodEnd = periodData.get("end_time")?.asString ?: ""
                            val lessons = periodData.getAsJsonArray("lessons")
                            
                            val isNow = periodData.get("is_now")?.asBoolean ?: false
                            
                            if (lessons != null && lessons.size() > 0) {
                                for (lessonElement in lessons) {
                                    val lesson = lessonElement.asJsonObject
                                    val teachers = lesson.getAsJsonArray("teachers")
                                    val teacherStr = teachers?.joinToString(", ") { it.asString } ?: ""
                                    
                                    timetable.add(
                                        TimetableEntry(
                                            period = periodName,
                                            timeStart = periodStart,
                                            timeEnd = periodEnd,
                                            subject = lesson.get("subject_name")?.asString ?: "",
                                            className = lesson.get("lesson_class_name")?.asString ?: "",
                                            teacher = teacherStr,
                                            room = lesson.get("room_name")?.asString ?: "",
                                            bgColor = "#${lesson.get("class_background_colour")?.asString ?: "FFFFFF"}",
                                            borderColor = "#${lesson.get("class_border_colour")?.asString ?: "000000"}",
                                            isCurrent = isNow
                                        )
                                    )
                                }
                            } else if (periodName.isNotEmpty() && !listOf("RC", "R1", "R2", "L1", "L2", "7", "8", "9").contains(periodName)) {
                                 // 空闲课程
                                 timetable.add(
                                    TimetableEntry(
                                        period = periodName,
                                        timeStart = periodStart,
                                        timeEnd = periodEnd,
                                        subject = "No Lesson",
                                        className = "",
                                        teacher = "",
                                        room = "",
                                        bgColor = "",
                                        borderColor = "",
                                        isCurrent = isNow,
                                        isFree = true
                                    )
                                )
                            }
                        }
                        // 找到日期后就可以结束了
                        break
                    }
                }
                if (timetable.isNotEmpty()) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 按时间排序
        return timetable.sortedWith(compareBy<TimetableEntry> { 
            try {
                val parts = it.timeStart.trim().split(":")
                if (parts.size >= 2) {
                    parts[0].toInt() * 60 + parts[1].toInt()
                } else {
                     when (it.period) {
                        "7" -> -20
                        "8" -> 2000
                        else -> Int.MAX_VALUE
                    }
                }
            } catch (e: Exception) {
                when (it.period) {
                    "7" -> -20
                    "8" -> 2000
                    else -> Int.MAX_VALUE
                }
            }
        }.thenBy { 
            val p = it.period
            when {
                p.toIntOrNull() != null -> p.toInt() * 10
                p == "RC" -> 25
                else -> 100
            }
        })
    }

    /**
     * 检查登录状态
     */
    suspend fun checkLoginStatus(): Boolean = withContext(Dispatchers.IO) {
        val cookie = cookieManager.getCookie() ?: return@withContext false
        val sessionId = cookieManager.getSessionId() ?: return@withContext false
        
        // 简单检查 /portal/user 是否返回 200
        val url = "$baseUrl/s-$sessionId/portal/user"
        
        try {
            val request = Request.Builder()
                .url(url)
                .header("Cookie", cookie)
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 根据 Cookie 自动发现 Session ID 和 Student ID
     */
    suspend fun discoverSessionInfo(cookie: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
             // 保持原有逻辑不变，或者也可以利用 /portal/user 简化 studentId 获取
             // 这里为了稳妥，暂不大幅修改 discoverSessionInfo，除非必要
            var sessionId: String? = null
            var studentId: String? = null
            
             try {
                val request = Request.Builder()
                    .url("$baseUrl/portal")
                    .header("Cookie", cookie)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                
                val response = client.newCall(request).execute()
                val finalUrl = response.request.url.toString()
                val bodyString = response.body?.string() ?: ""
                
                val sessionIdMatch = Regex("/s-([a-zA-Z0-9]+)/").find(finalUrl)
                sessionId = sessionIdMatch?.groupValues?.get(1)
                
                // 尝试从 /portal/user 获取 ID
                 if (sessionId != null) {
                     val userUrl = "$baseUrl/s-$sessionId/portal/user"
                     val userRequest = Request.Builder().url(userUrl).header("Cookie", cookie).build()
                     val userResp = client.newCall(userRequest).execute()
                     if (userResp.isSuccessful) {
                         val userJson = gson.fromJson(userResp.body?.string(), JsonObject::class.java)
                         studentId = userJson.get("student_id")?.asString ?: userJson.get("id")?.asString
                     }
                 }
                 
                 // 如果还没有 studentId，使用旧的正则匹配作为后备
                if (studentId == null) {
                    studentId = Regex("data-student-id=\"(\\d+)\"").find(bodyString)?.groupValues?.get(1)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
             
            if (sessionId != null && studentId != null) {
                Pair(sessionId, studentId)
            } else {
                null
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 清除缓存数据
     */
    fun clearCache() {
        cachedTimetable = null
    }
}
