package com.example.data

import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

data class MailSettings(
    val smtpHost: String = "",
    val smtpPort: Int = 465,
    val imapHost: String = "",
    val imapPort: Int = 993,
    val user: String = "",
    val pass: String = "",
    val useSsl: Boolean = true
)

object MailTransport {

    fun sendEmail(settings: MailSettings, recipient: String, subject: String, body: String): Result<String> {
        if (settings.smtpHost.isBlank() || settings.user.isBlank() || settings.pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Отсутствуют настройки SMTP-сервера"))
        }
        return try {
            val socket = if (settings.useSsl) {
                SSLSocketFactory.getDefault().createSocket(settings.smtpHost, settings.smtpPort)
            } else {
                Socket(settings.smtpHost, settings.smtpPort)
            }
            socket.soTimeout = 8000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            fun readResponse(): String {
                val line = reader.readLine() ?: ""
                var last = line
                while (last.length > 3 && last[3] == '-') {
                    last = reader.readLine() ?: ""
                }
                return last
            }

            val greeting = readResponse() // 220
            if (!greeting.startsWith("220")) {
                socket.close()
                return Result.failure(Exception("SMTP приветствие не получено: $greeting"))
            }

            writer.println("EHLO localhost")
            readResponse() // 250

            writer.println("AUTH LOGIN")
            val authPrompt = readResponse() // 334
            if (!authPrompt.startsWith("334")) {
                socket.close()
                return Result.failure(Exception("SMTP AUTH LOGIN отвергнут: $authPrompt"))
            }

            val userB64 = Base64.encodeToString(settings.user.toByteArray(), Base64.NO_WRAP)
            writer.println(userB64)
            val passPrompt = readResponse() // 334

            val passB64 = Base64.encodeToString(settings.pass.toByteArray(), Base64.NO_WRAP)
            writer.println(passB64)
            val authStatus = readResponse() // 235
            if (!authStatus.startsWith("235")) {
                socket.close()
                return Result.failure(Exception("SMTP авторизация не удалась: $authStatus"))
            }

            writer.println("MAIL FROM:<${settings.user}>")
            readResponse()

            writer.println("RCPT TO:<$recipient>")
            readResponse()

            writer.println("DATA")
            val dataStatus = readResponse()
            if (!dataStatus.startsWith("354")) {
                socket.close()
                return Result.failure(Exception("SMTP DATA отвергнут: $dataStatus"))
            }

            writer.println("From: ${settings.user}")
            writer.println("To: $recipient")
            writer.println("Subject: $subject")
            writer.println("Content-Type: text/plain; charset=UTF-8")
            writer.println()
            writer.println(body)
            writer.println(".")
            val finalStatus = readResponse()

            writer.println("QUIT")
            readResponse()

            socket.close()

            if (finalStatus.startsWith("250")) {
                Result.success("Трансляция пира успешно осуществлена по протоколу SMTP")
            } else {
                Result.failure(Exception("Ошибка отправки SMTP конверта: $finalStatus"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fetchUnreadP2PMessages(settings: MailSettings): Result<List<String>> {
        if (settings.imapHost.isBlank() || settings.user.isBlank() || settings.pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Отсутствуют настройки IMAP-сервера"))
        }
        return try {
            val socket = if (settings.useSsl) {
                SSLSocketFactory.getDefault().createSocket(settings.imapHost, settings.imapPort)
            } else {
                Socket(settings.imapHost, settings.imapPort)
            }
            socket.soTimeout = 10000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            fun readUntilTag(tag: String): List<String> {
                val lines = mutableListOf<String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    lines.add(line)
                    if (line.startsWith(tag)) break
                }
                return lines
            }

            val greeting = reader.readLine() ?: ""
            if (!greeting.contains("OK")) {
                socket.close()
                return Result.failure(Exception("IMAP сервер не готов: $greeting"))
            }

            writer.println("A1 LOGIN \"${settings.user}\" \"${settings.pass}\"")
            val loginResp = readUntilTag("A1")
            if (!loginResp.lastOrNull().orEmpty().contains("OK")) {
                socket.close()
                return Result.failure(Exception("IMAP авторизация не удалась: ${loginResp.joinToString("\n")}"))
            }

            writer.println("A2 SELECT INBOX")
            val selectResp = readUntilTag("A2")
            if (!selectResp.lastOrNull().orEmpty().contains("OK")) {
                socket.close()
                return Result.failure(Exception("IMAP выбор INBOX не удался"))
            }

            // Ищем сообщения по теме ORDEN-P2P
            writer.println("A3 SEARCH SUBJECT \"ORDEN-P2P\"")
            val searchResp = readUntilTag("A3")
            var msgIds = listOf<String>()
            for (line in searchResp) {
                if (line.startsWith("* SEARCH")) {
                    msgIds = line.substringAfter("* SEARCH").trim().split(" ").filter { it.isNotEmpty() }
                }
            }

            val envelopes = mutableListOf<String>()
            // Извлекаем последние 8 писем
            val targetIds = msgIds.takeLast(8)
            for (mid in targetIds) {
                writer.println("A4 FETCH $mid BODY[TEXT]")
                val fetchLines = mutableListOf<String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("A4 OK")) break
                    fetchLines.add(line)
                }
                val fullText = fetchLines.joinToString("\n")
                if (fullText.contains("ORDEN-P2P-PROTOCOL-V1")) {
                    envelopes.add(fullText)
                }
            }

            writer.println("A5 LOGOUT")
            readUntilTag("A5")
            socket.close()

            Result.success(envelopes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
