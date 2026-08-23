package msr.atsulab.app.player.ui

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PlayerDebugCrashRecorder {

    private val installLock = Any()

    @Volatile
    private var installed = false

    fun install(context: Context) {
        val appContext = context.applicationContext
        synchronized(installLock) {
            if (installed) return@synchronized
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching { persist(appContext, thread, throwable) }
                previousHandler?.uncaughtException(thread, throwable)
            }
            installed = true
        }
    }

    fun persist(context: Context, thread: Thread, throwable: Throwable): File {
        return writeReport(context, buildReport(thread, throwable))
    }

    fun read(context: Context): String? {
        val file = reportFile(context)
        return if (file.isFile && file.length() > 0L) file.readText() else null
    }

    fun clear(context: Context) {
        reportFile(context).delete()
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val stackTrace = java.io.StringWriter().also { writer ->
            throwable.printStackTrace(java.io.PrintWriter(writer))
        }.toString()

        return buildString {
            appendLine("AtsuLab HLS Debug Crash")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            append(stackTrace)
        }
    }

    private fun writeReport(context: Context, report: String): File {
        val file = reportFile(context)
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(report)
        if (file.exists()) file.delete()
        if (!temporaryFile.renameTo(file)) {
            file.writeText(report)
            temporaryFile.delete()
        }
        return file
    }

    private fun reportFile(context: Context): File {
        return File(context.applicationContext.filesDir, FILE_NAME)
    }

    private const val FILE_NAME = "atsu_player_debug_crash.txt"
}
