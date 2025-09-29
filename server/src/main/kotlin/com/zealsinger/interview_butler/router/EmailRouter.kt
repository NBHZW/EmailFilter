package com.zealsinger.interview_butler.router

import EventDataManager.Companion.eventListToJson
import com.zealsinger.interview_butler.cache.EmailCache
import com.zealsinger.interview_butler.domain.EmailEnty
import com.zealsinger.interview_butler.session.SessionUtil
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.MimeMessage
import javax.mail.search.AndTerm
import javax.mail.search.ComparisonTerm
import javax.mail.search.ReceivedDateTerm


@OptIn(InternalAPI::class)
fun Route.EmailRouter() {
    route("/email") {
        get("/findAllEmail") {
            SessionUtil.currentFolder?.let { folder ->
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -5) // 一天前的日期
                val startTime = calendar.time
                val endTime = Date()
                val cache = EmailCache.getCache(
                    startTime.toLocalDateTime().toLocalDate(),
                    endTime.toLocalDateTime().toLocalDate()
                )
                if (!cache.isEmpty()) {
                    call.respond(eventListToJson(cache.toList()))
                    return@get
                }

                val comparisonTermGe = ReceivedDateTerm(ComparisonTerm.GE, startTime)
                val comparisonTermLe = ReceivedDateTerm(ComparisonTerm.LE, endTime)
                val comparisonAndTerm = AndTerm(comparisonTermGe, comparisonTermLe)

                /**
                 *  过滤条件均使用receiveDate即接收时间才行 否则会有问题
                 *  因为整个流程是 A发送方服务器--->QQ IMAP服务器中对应的我们的邮箱 --->C 本服务
                 *  发送消息由A确定，接受时间由IMAP服务器确定，因为发送方机器多样性，地域多样性导致了发送时间存在差异，而且可能是很大的差异
                 *  所以这里使用receiveDate进行过滤，统一以IMAP服务器的时间为基准进行过滤 筛选和排序
                 */
                val messages = folder.search(comparisonAndTerm)
                val sortedMessages = messages.sortedWith(compareByDescending { it.receivedDate })
                val emailList = processEmailsParallel(sortedMessages)
                val resultList = emailList.sorted()

                val jsonContent = Json.encodeToString(resultList)
                val aiResponse = sendPostRequest(jsonContent).replace("^'''JSON\\s*|\\s*'''$".toRegex(), "")
                // 异步更新缓存
                EmailCache.executor.execute { EmailCache.addCache(aiResponse) }
                call.respond(
                    aiResponse
                )
            }
        }
    }
}

// 使用协程并行处理
fun processEmailsParallel(messages: List<Message>): List<EmailEnty> {
    val emailList = mutableListOf<EmailEnty>()

    runBlocking {
        val deferredList = (0 until minOf(messages.size, 10)).map { i ->
            async {
                val message = messages[i] as MimeMessage
                parseEmailMessage(message)
            }
        }

        emailList.addAll(deferredList.awaitAll().filterNotNull())
    }

    return emailList
}

fun parseEmailMessage(message: MimeMessage): EmailEnty? {
    if(message.from == null || message.subject == null || message.content == null || message.allRecipients == null )  null
    val fromStart = message.from?.joinToString()?.indexOf('<')?: 0
    val fromEnd = message.from?.joinToString()?.indexOf('>')?: message.from?.toString()?.length?:0
    val from = message.from?.joinToString()?.substring(fromStart + 1, fromEnd)?: ""

    val toStart = message.allRecipients?.joinToString()?.indexOf('<')?: 0
    val toEnd = message.allRecipients?.joinToString()?.indexOf('>')?: message.from?.toString()?.length?:0
    val to = message.allRecipients?.joinToString()?.substring(toStart + 1, toEnd)?: ""

    val stringBuffer = StringBuffer(30)
    getMailTextContent(message, stringBuffer)
    val email = EmailEnty(
        message.messageID ?: message.receivedDate?.time.toString(), // 使用messageID或时间
        from,
        to,
        message.subject ?: "",
        message.sentDate?.toString() ?: "",
        stringBuffer.toString(),
    )
    return email
}


/**
 * 发送POST请求到AI服务
 */
fun sendPostRequest(jsonContent: String): String {
    val url = URL("http://localhost:8090/v6/ai/generate")
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

    val outputStream = connection.outputStream
    val writer = OutputStreamWriter(outputStream, "UTF-8")
    writer.write(jsonContent)
    writer.flush()
    writer.close()
    outputStream.close()

    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        return connection.inputStream.bufferedReader().use { it.readText() }
    } else {
        throw Exception("HTTP error: $responseCode")
    }
}

/**
 *
 * 递归提取邮件文本内容：
 * 文本类型处理：如果是纯文本且不包含附件名称，则直接添加到内容缓冲区
 * 嵌套邮件处理：如果是RFC822格式的嵌套邮件，则递归解析其内容
 * 多部分处理：如果是多部分邮件，则遍历每个部分并递归提取文本内容
 * 代码通过MIME类型判断和递归调用，能够处理复杂的邮件结构，最终将所有文本内容拼接到StringBuffer中。
 *
 */
@Throws(MessagingException::class, IOException::class)
fun getMailTextContent(part: Part, content: StringBuffer) {
    //如果是文本类型的附件，通过getContent方法可以取到文本内容，但这不是我们需要的结果，所以在这里要做判断
    val isContainTextAttach = part.contentType.indexOf("name") > 0
    if (part.isMimeType("text/*") && !isContainTextAttach) {
        content.append(part.content.toString())
    } else if (part.isMimeType("message/rfc822")) {
        getMailTextContent((part.content as Part?)!!, content)
    } else if (part.isMimeType("multipart/*")) {
        val multipart = part.content as Multipart
        val partCount = multipart.getCount()
        for (i in 0..<partCount) {
            val bodyPart = multipart.getBodyPart(i)
            getMailTextContent(bodyPart, content)
        }
    }
}