package com.sentral.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sentral.app.model.TimetableEntry
import java.io.File
import java.util.Date

class CacheManager(private val context: Context) {
    private val gson = Gson()
    private val cacheFile = File(context.cacheDir, "timetable_cache.json")

    fun saveTimetable(entries: List<TimetableEntry>) {
        try {
            val json = gson.toJson(entries)
            cacheFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTimetable(): List<TimetableEntry>? {
        return try {
            if (!cacheFile.exists()) return null
            val json = cacheFile.readText()
            val type = object : TypeToken<List<TimetableEntry>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasCache(): Boolean = cacheFile.exists()
}
