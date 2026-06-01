package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object DiagnosticsTracker {
    data class LogEntry(
        val timestamp: Long,
        val tag: String,
        val message: String,
        val severity: String = "INFO",
        val errorDetails: String? = null
    )

    private val logs = CopyOnWriteArrayList<LogEntry>()
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null
    private val logFileName = "app_errors.log"

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        
        // Load persist log from storage
        loadLogsFromFile()

        // Hook exception handler
        try {
            defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                logError("UncaughtException", "Uncaught exception on thread ${thread.name}", throwable)
                // Persist the crash immediately to file
                saveLogsToFile()
                
                // Pass back to original handler to let Android process crash normally
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }
            logInfo("DiagnosticsTracker", "DiagnosticsTracker configured successfully.")
        } catch (e: Exception) {
            Log.e("DiagnosticsTracker", "Failed to hook uncaught exception handler", e)
        }
    }

    fun logInfo(tag: String, message: String) {
        logs.add(LogEntry(System.currentTimeMillis(), tag, message, "INFO"))
        Log.i(tag, message)
        saveLogsToFile()
    }

    fun logWarning(tag: String, message: String) {
        logs.add(LogEntry(System.currentTimeMillis(), tag, message, "WARN"))
        Log.w(tag, message)
        saveLogsToFile()
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val details = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }
        logs.add(LogEntry(System.currentTimeMillis(), tag, message, "ERROR", details))
        Log.e(tag, "$message: $details")
        saveLogsToFile()
    }

    fun getRecentLogs(): List<LogEntry> = logs.toList()

    private fun loadLogsFromFile() {
        val context = appContext ?: return
        try {
            val file = File(context.filesDir, logFileName)
            if (file.exists()) {
                val lines = file.readLines()
                // Parse lines and add to list. 
                // Line format of file: TIMEOUT|SEVERITY|TAG|MESSAGE|DETAILS_HEX
                for (line in lines) {
                    val parts = line.split("|", limit = 5)
                    if (parts.size >= 4) {
                        val timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis()
                        val severity = parts[1]
                        val tag = parts[2]
                        val message = parts[3]
                        val details = if (parts.size == 5 && parts[4].isNotEmpty()) {
                            // Decode from hex sequence to prevent breaking newlines in simple single-line log format
                            String(hexStringToByteArray(parts[4]))
                        } else null
                        
                        logs.add(LogEntry(timestamp, tag, message, severity, details))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DiagnosticsTracker", "Error loading persistent logs", e)
        }
    }

    private fun saveLogsToFile() {
        val context = appContext ?: return
        try {
            val file = File(context.filesDir, logFileName)
            val currentLogs = logs.toList()
            val fileLines = currentLogs.map { entry ->
                val detailsHex = entry.errorDetails?.let { byteArrayToHexString(it.toByteArray()) } ?: ""
                "${entry.timestamp}|${entry.severity}|${entry.tag}|${entry.message}|$detailsHex"
            }
            file.writeText(fileLines.joinToString("\n"))
        } catch (e: Exception) {
            Log.e("DiagnosticsTracker", "Error writing persistent logs", e)
        }
    }

    // Hex helper methods to safely store multi-line stacktraces in single line records
    private fun byteArrayToHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { String.format("%02X", it) }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun clearAllLogs() {
        logs.clear()
        val context = appContext ?: return
        try {
            val file = File(context.filesDir, logFileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("DiagnosticsTracker", "Error clearing persistent logs", e)
        }
    }

    fun getDiagnosticsReport(apiKeyStatus: String, activeModel: String): String {
        val report = StringBuilder()
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        
        report.append("=========================================\n")
        report.append("      OPEN ORDEN SYSTEM DIAGNOSTICS\n")
        report.append("=========================================\n\n")
        
        report.append("--- RUNTIME / DEVICE ENVIRONMENT ---\n")
        report.append("Device Board/Brand: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        report.append("Android Rel Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        report.append("Hardware Platform: ${Build.BOARD} / ${Build.HARDWARE}\n")
        report.append("Diagnostics Time: ${format.format(Date())}\n\n")
        
        report.append("--- AI COGNITIVE AGENT CONFIGURATION ---\n")
        report.append("Active LLM Model: $activeModel\n")
        report.append("Gemini API Key: $apiKeyStatus\n")
        report.append("Endpoint URL: https://generativelanguage.googleapis.com/\n\n")
        
        report.append("--- CAPTURED DIAGNOSTIC EXCEPTIONS (Recent First) ---\n")
        val errorLogs = logs.toList().filter { it.severity == "ERROR" }.reversed()
        if (errorLogs.isEmpty()) {
            report.append("No active or historical errors logged in this session.\nSystem is completely healthy. 🌌\n")
        } else {
            errorLogs.forEachIndexed { index, entry ->
                report.append(" [ERROR #${index + 1}] • ${format.format(Date(entry.timestamp))} • Tag: ${entry.tag}\n")
                report.append("  Message: ${entry.message}\n")
                if (entry.errorDetails != null) {
                    report.append("  Stacktrace:\n${entry.errorDetails}\n")
                }
                report.append("--------------------------------------------------\n")
            }
        }
        
        report.append("\n--- CONSOLE OPERATIONS AND SYNC EVENTS (Last 15) ---\n")
        val recentLogs = logs.toList().reversed().take(15)
        if (recentLogs.isEmpty()) {
            report.append("Empty activity log.\n")
        } else {
            recentLogs.forEach { entry ->
                val time = format.format(Date(entry.timestamp))
                report.append(" [$time] [${entry.severity}] [${entry.tag}]: ${entry.message}\n")
            }
        }
        
        report.append("\n=========================================\n")
        report.append("END OF SYSTEM DIAGNOSTICS LOG. Copy / paste this to AI Agent for instant fixes!\n")
        report.append("=========================================\n")
        
        return report.toString()
    }

    fun shareDiagnostics(context: Context, reportText: String) {
        try {
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                type = "text/plain"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "Отправить отчет диагностики")
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e("DiagnosticsTracker", "Failed to start share intent", e)
        }
    }
}
