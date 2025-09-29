package com.zealsinger.interview_butler.session

import java.util.Properties
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store

/**
 *
 */
class SessionUtil {
    companion object {
        private var store: Store? = null
        private var folder: Folder? = null

        init {
            initialize()
            Runtime.getRuntime().addShutdownHook(Thread {
                closeResources()
            })
        }

        val currentFolder: Folder?
            get() {
                if (folder == null) {
                    initialize()
                }
                return folder
            }

        private fun initialize() {
            val HOST = "imap.qq.com"
            val userName = "2397558296@qq.com"
            val password = "pvbpchwflqbdecdh"

            val properties = Properties().apply {
                put("mail.store.protocol", "imap")
                put("mail.imap.host", HOST)
                put("mail.imap.port", "993")
                put("mail.imap.ssl.enable", "true")
                put("mail.imap.ssl.protocols", "TLSv1.2 TLSv1.3")
                put("mail.imap.starttls.enable", "false") // 禁用STARTTLS，使用纯SSL
                put("mail.imap.auth", "true")
                put("mail.imap.auth.plain.disable", "true")
                // 增加超时设置
                put("mail.imap.connectiontimeout", "10000")
                put("mail.imap.timeout", "10000")
                put("mail.imap.writetimeout", "10000")
                // 启用调试以获取更多信息
                put("mail.debug", "true")
            }

            val session = Session.getInstance(properties, null)
            store = session.getStore("imap")
            store!!.connect(HOST, userName, password)
            folder = store!!.getFolder("INBOX")
            folder!!.open(Folder.READ_WRITE)
        }

        fun closeResources() {
            try {
                folder?.close(false)
                store?.close()
            } catch (e: Exception) {
                // 记录日志
            }
        }

        // 可选：手动检查并重新连接的方法
        fun checkConnection() {
            if (folder == null || !folder!!.isOpen) {
                closeResources()
                initialize()
            }
        }
    }
}