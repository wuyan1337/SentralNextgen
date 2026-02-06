package com.sentral.app.data

import android.content.Context
import android.content.SharedPreferences

class TaskManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sentral_tasks", Context.MODE_PRIVATE)

    // Key: Subject Name
    // Value: Task Content
    fun saveTask(subject: String, task: String) {
        if (task.isBlank()) {
            prefs.edit().remove(subject).apply()
        } else {
            prefs.edit().putString(subject, task).apply()
        }
    }

    fun getTask(subject: String): String? {
        return prefs.getString(subject, null)
    }
    
    // Check if task exists for UI indicator
    fun hasTask(subject: String): Boolean {
        return !prefs.getString(subject, null).isNullOrEmpty()
    }
}
