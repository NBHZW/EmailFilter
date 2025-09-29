package com.zealsinger.interview_butler.cache

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Event(
    val id: String,
    val company: String,
    val position: String,
    val type: String,
    val link: String,

    @SerializedName("scheduledTime")
    val scheduledTimeStr: String,

    @SerializedName("emailReceiveTime")
    val emailReceiveTimeStr: String,

    val color: String
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    val scheduledTime: LocalDateTime?
        get() = scheduledTimeStr.takeIf { it.isNotBlank() }?.let {
            LocalDateTime.parse(it, dateTimeFormatter)
        }

    val emailReceiveTime: LocalDateTime
        get() = LocalDateTime.parse(emailReceiveTimeStr, dateTimeFormatter)

    val isExpired: Boolean
        get() = scheduledTime?.isBefore(LocalDateTime.now()) ?: false

    val isToday: Boolean
        get() = scheduledTime?.toLocalDate() == LocalDateTime.now().toLocalDate()

    val isUpcoming: Boolean
        get() = scheduledTime?.isAfter(LocalDateTime.now()) ?: false

    // 获取距离事件还有多少天
    val daysUntilEvent: Long?
        get() = scheduledTime?.toLocalDate()?.let { eventDate ->
            java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                eventDate
            )
        }

    // 格式化显示时间
    val formattedScheduledTime: String
        get() = scheduledTime?.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm")) ?: "未安排时间"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}