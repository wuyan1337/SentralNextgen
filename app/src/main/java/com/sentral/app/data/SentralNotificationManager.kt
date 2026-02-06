package com.sentral.app.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sentral.app.MainActivity
import com.sentral.app.R
import com.sentral.app.model.TimetableEntry
import com.sentral.app.ui.widget.TimetableWidget
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra("subject") ?: "Class"
        val room = intent.getStringExtra("room") ?: ""
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "class_alerts"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Class Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val contentText = if (room.isNotEmpty()) "Location: $room" else "Starting soon"
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Fallback icon
            .setContentTitle("Next Class: $subject")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class SentralNotificationManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun scheduleNotifications(timetable: List<TimetableEntry>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return 
        }

        val now = Calendar.getInstance()
        
        timetable.forEach { entry ->
            if (entry.isFree) return@forEach
            
            val parts = entry.timeStart.split(":")
            if (parts.size != 2) return@forEach
            
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            
            // 1. Class Notification (5 mins before)
            val notifyCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                add(Calendar.MINUTE, -5)
            }
            
            val classStartRandom = Calendar.getInstance().apply {
                 set(Calendar.HOUR_OF_DAY, hour)
                 set(Calendar.MINUTE, minute)
                 set(Calendar.SECOND, 0)
            }

            // Logic:
            // If now < notifyTime -> Schedule normal alarm
            // If now >= notifyTime BUT now < startTime -> Schedule immediate alarm (missed window but still relevant)
            
            if (now.before(classStartRandom)) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("subject", entry.subject)
                    putExtra("room", entry.room)
                }
                val reqCode = hour * 100 + minute
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    reqCode, 
                    intent, 
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val triggerTime = if (now.before(notifyCalendar)) {
                    notifyCalendar.timeInMillis
                } else {
                    // Missed the 5-min window, but class hasn't started. Trigger immediately.
                    System.currentTimeMillis()
                }

                try {
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
        
        scheduleWidgetUpdates(timetable)
    }

    private fun scheduleWidgetUpdates(timetable: List<TimetableEntry>) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return 
        }
        
        val now = Calendar.getInstance()
        
        timetable.forEach { entry ->
            if (entry.isFree) return@forEach
             try {
                // Parse Times
                val startParts = entry.timeStart.split(":")
                val endParts = entry.timeEnd.split(":")
                
                val startHour = startParts[0].toInt()
                val startMinute = startParts[1].toInt()
                val endHour = endParts[0].toInt()
                val endMinute = endParts[1].toInt()

                // Schedule Update at Start Time
                scheduleWidgetUpdate(startHour, startMinute, now)
                
                // Schedule Update at End Time
                scheduleWidgetUpdate(endHour, endMinute, now)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun scheduleWidgetUpdate(hour: Int, minute: Int, now: Calendar) {
        val updateTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0) // Precision
        }
        
        // Only schedule future updates
        if (updateTime.after(now)) {
            val intent = Intent(context, TimetableWidget::class.java).apply {
                action = TimetableWidget.ACTION_AUTO_UPDATE
            }
            
            // Unique Request Code for Widget Updates: 10000 offset + time
            val reqCode = 10000 + (hour * 100 + minute)
            
            // Note: If start and end times collide for different classes (e.g. one ends 9:00, next starts 9:00),
            // the reqCode will be same. This is GOOD. We only need one update trigger at 9:00.
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, updateTime.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, updateTime.timeInMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
