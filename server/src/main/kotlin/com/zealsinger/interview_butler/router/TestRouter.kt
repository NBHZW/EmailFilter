package com.zealsinger.interview_butler.router

import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors


fun Route.TestRouter() {
    route("/test") {
        get("/onlyVirtualThreads") {
            val resultList = mutableListOf<Int>()
            val startTime = System.currentTimeMillis()
            runBlocking {
                val deferredList = List(1000) { index ->
                    async {
                        // 模拟1-2分钟的耗时操作
                        val randomDelay = (60000..90000).random()
                        delay(randomDelay.toLong())
                        index
                    }
                }
                val results = deferredList.awaitAll()
                resultList.addAll(results)
            }
            call.respond("时间开销：${System.currentTimeMillis() - startTime}ms")
        }


        get("/onlyCoroutines") {
            val resultList = mutableListOf<Int>()
            val executor = Executors.newVirtualThreadPerTaskExecutor()

            val startTime = System.currentTimeMillis()
            val futures = List(1000) { index ->
                executor.submit<Int> {
                    // 模拟1-2分钟的耗时操作
                    val randomDelay = (60000..90000).random()
                    Thread.sleep(randomDelay.toLong())
                    index
                }
            }
            val results = futures.map { it.get() }
            resultList.addAll(results)
            executor.close()
            call.respond("时间开销：${System.currentTimeMillis() - startTime}ms")
        }

        get("/coroutinesWithVirtualThreads") {
            val resultList = mutableListOf<Int>()
            val startTime = System.currentTimeMillis()

            runBlocking {
                // 创建虚拟线程调度器
                val virtualThreadDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

                val deferredList = List(1000) { index ->
                    async(virtualThreadDispatcher) {
                        // 模拟1-2分钟的耗时操作 - 使用Thread.sleep而不是delay
                        // 因为我们在虚拟线程上运行，Thread.sleep不会阻塞平台线程
                        val randomDelay = (60000..90000).random()
                        delay(randomDelay.toLong())
                        index
                    }
                }
                val results = deferredList.awaitAll()
                resultList.addAll(results)

                // 关闭执行器释放资源
                (virtualThreadDispatcher.executor as? java.util.concurrent.ExecutorService)?.shutdown()
            }
            call.respond("时间开销：${System.currentTimeMillis() - startTime}ms")
        }
    }
}