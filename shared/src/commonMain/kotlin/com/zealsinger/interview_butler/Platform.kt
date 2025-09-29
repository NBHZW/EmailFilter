package com.zealsinger.interview_butler

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform