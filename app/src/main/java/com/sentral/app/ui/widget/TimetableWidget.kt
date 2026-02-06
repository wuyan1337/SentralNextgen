package com.sentral.app.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sentral.app.R
import com.sentral.app.data.CacheManager
import com.sentral.app.model.TimetableEntry
import java.util.Calendar

class TimetableWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Iterate over all instances of this widget
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_AUTO_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = android.content.ComponentName(context.packageName, TimetableWidget::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_AUTO_UPDATE = "com.sentral.app.ACTION_AUTO_UPDATE"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                
                val cacheManager = CacheManager(context)
                val entries = cacheManager.getTimetable()
                val strings = com.sentral.app.ui.res.getAppStrings(context)
                
                // Default Visibility
                views.setViewVisibility(R.id.section_current, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_divider, android.view.View.GONE)
                views.setViewVisibility(R.id.section_next, android.view.View.VISIBLE) // Always keep one visible container

                if (entries.isNullOrEmpty()) {
                    // Show "No Data" in Next Section
                    views.setTextViewText(R.id.label_next, strings.appName)
                    views.setTextViewText(R.id.subject_next, strings.widgetNoData)
                    views.setTextViewText(R.id.time_next, strings.widgetOpenApp)
                    views.setTextViewText(R.id.room_next, "")
                    views.setViewVisibility(R.id.room_next, android.view.View.GONE)
                } else {
                    val (currentClass, nextClass) = findCurrentAndNextClass(entries)
                    
                    // 1. Setup Current Class
                    if (currentClass != null) {
                        views.setViewVisibility(R.id.section_current, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.label_current, strings.widgetNow)
                        views.setTextViewText(R.id.subject_current, currentClass.subject)
                        views.setTextViewText(R.id.time_current, "${currentClass.timeStart} - ${currentClass.timeEnd}")
                        
                        if (currentClass.room.isNotEmpty()) {
                             views.setTextViewText(R.id.room_current, currentClass.room)
                             views.setViewVisibility(R.id.room_current, android.view.View.VISIBLE)
                        } else {
                             views.setViewVisibility(R.id.room_current, android.view.View.GONE)
                        }
                    }
                    
                    // 2. Setup Next Class
                    if (nextClass != null) {
                        views.setViewVisibility(R.id.section_next, android.view.View.VISIBLE)
                        views.setTextViewText(R.id.label_next, strings.widgetNext)
                        views.setTextViewText(R.id.subject_next, nextClass.subject)
                        views.setTextViewText(R.id.time_next, "${nextClass.timeStart} - ${nextClass.timeEnd}")
                        
                        if (nextClass.room.isNotEmpty()) {
                             views.setTextViewText(R.id.room_next, nextClass.room)
                             views.setViewVisibility(R.id.room_next, android.view.View.VISIBLE)
                        } else {
                             views.setViewVisibility(R.id.room_next, android.view.View.GONE)
                        }
                    } else if (currentClass == null) {
                        // No Current AND No Next -> All Done
                        views.setTextViewText(R.id.label_next, strings.appName)
                        views.setTextViewText(R.id.subject_next, strings.widgetAllDone)
                        views.setTextViewText(R.id.time_next, strings.widgetNoMoreClasses)
                        views.setTextViewText(R.id.room_next, strings.widgetKeepItUp)
                        views.setViewVisibility(R.id.room_next, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.room_next, android.view.View.GONE)
                    } else {
                        // Has Current, but No Next (Last class of day)
                        views.setViewVisibility(R.id.section_next, android.view.View.GONE)
                    }
                    
                    // 3. Divider
                    if (currentClass != null && nextClass != null) {
                        views.setViewVisibility(R.id.widget_divider, android.view.View.VISIBLE)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        fun findCurrentAndNextClass(entries: List<TimetableEntry>): Pair<TimetableEntry?, TimetableEntry?> {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentTime = currentHour * 60 + currentMinute
            
            var current: TimetableEntry? = null
            var next: TimetableEntry? = null
            
            for (entry in entries) {
                if (entry.isFree) continue
                
                try {
                    val startParts = entry.timeStart.split(":")
                    val endParts = entry.timeEnd.split(":")
                    
                    val startTime = startParts[0].toInt() * 60 + startParts[1].toInt()
                    val endTime = endParts[0].toInt() * 60 + endParts[1].toInt()
                    
                    // Check if Current
                    if (currentTime in startTime until endTime) {
                        current = entry
                    }
                    
                    // Check if Next (First one found that starts after now)
                    if (next == null && startTime > currentTime) {
                        next = entry
                    }
                    
                    if (current != null && next != null) break
                    
                } catch (e: Exception) {
                    continue
                }
            }
            return Pair(current, next)
        }
    }
}
