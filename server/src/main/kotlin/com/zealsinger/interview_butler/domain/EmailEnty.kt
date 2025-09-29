package com.zealsinger.interview_butler.domain

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

@Serializable
class EmailEnty(
     val id: String,
     val from: String,
     val to: String,
     val subject: String,
     val date: String,
     val content: String
) : Comparable<EmailEnty> {
    companion object {
        // 创建日期格式化器，用于解析日期字符串
        val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.CHINESE)
    }

    override fun compareTo(other: EmailEnty): Int {
        // 首先按 to 字段的字典序比较
        val toComparison = this.to.compareTo(other.to)
        if (toComparison != 0) {
            return toComparison
        }

        // 如果 to 相同，则按日期比较
        return try {
            val thisDate = dateFormat.parse(this.date)
            val otherDate = dateFormat.parse(other.date)
            thisDate.compareTo(otherDate)
        } catch (e: Exception) {
            // 如果日期解析失败，回退到字符串比较
            this.date.compareTo(other.date)
        }
    }
}