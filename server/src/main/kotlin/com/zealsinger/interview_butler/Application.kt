package com.zealsinger.interview_butler

import com.zealsinger.interview_butler.ai.OpenRouterAI
import com.zealsinger.interview_butler.router.EmailRouter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.serialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        // 允许的域列表，可以使用"*"允许所有，但不推荐在生产环境中使用
        anyHost() // 允许任何主机，或者使用host("your-domain.com")指定特定主机
        // 允许的HTTP方法
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        // 允许的头部
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        // 是否允许凭据（如cookies）
        allowCredentials = true
        // 预检请求的最大存活时间（秒）
        maxAgeInSeconds = 24 * 60 * 60
    }
    routing {
        get("/") {
            val answer = OpenRouterAI().getAnswer("hello")
            call.respondText("Ktor: ${Greeting().greet()}\nAI: $answer")
        }
        EmailRouter()
    }
}