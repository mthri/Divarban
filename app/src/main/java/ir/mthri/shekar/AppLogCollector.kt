package ir.mthri.shekar

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogCollector {

    private val logMessages = StringBuilder()
    private const val MAX_LOG_LINES = 1000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Synchronized
    fun addLog(level: String, tag: String, message: String) {
        val currentTime = Date()
        val formattedTime = dateFormat.format(currentTime)
        val logLine = "$formattedTime $level/$tag: $message\n"
        logMessages.append(logLine)

    }

    @Synchronized
    fun getAllLogs(): String {
        return logMessages.toString()
    }

    @Synchronized
    fun clearLogs() {
        logMessages.clear()
    }
}