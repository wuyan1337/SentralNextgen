package com.sentral.app.model

/**
 * 课表数据模型
 */
data class TimetableEntry(
    val period: String,
    val timeStart: String,
    val timeEnd: String,
    val subject: String,
    val className: String,
    val teacher: String,
    val room: String,
    val bgColor: String,
    val borderColor: String,
    val isCurrent: Boolean = false,
    val isFree: Boolean = false
)

/**
 * 日期信息
 */
data class DateInfo(
    val dateName: String,
    val dayName: String
)
