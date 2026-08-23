package msr.atsulab.app.helper.crash

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.SystemClock
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CrashReporter {

    private const val REPORT_FILE = "atsulab_crash_report.txt"
    private const val TRACE_FILE = "atsulab_crash_trace.txt"
    private const val HISTORY_FILE = "atsulab_crash_history.txt"
    private const val MAX_BREADCRUMBS = 250
    private const val MAX_TRACE_BYTES = 512L * 1024L
    private const val MAX_HISTORY_BYTES = 2L * 1024L * 1024L
    private const val TRIM_KEEP_BYTES = 128L * 1024L

    private val lock = Any()
    private val breadcrumbs = ArrayDeque<String>()
    private val processStartElapsedRealtime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Process.getStartElapsedRealtime()
    } else {
        SystemClock.elapsedRealtime()
    }

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var currentScreen = "Application"

    @Volatile
    private var lastAction = "none"

    fun install(application: Application) {
        synchronized(lock) {
            appContext = application.applicationContext
            breadcrumb("app_create", "process_started")
            if (Thread.getDefaultUncaughtExceptionHandler() is CrashReporterHandler) return@synchronized

            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashReporterHandler(previousHandler))
        }
    }

    fun setCurrentScreen(name: String) {
        currentScreen = name.ifBlank { "Unknown" }
        breadcrumb("screen_current", currentScreen)
    }

    fun event(name: String, details: String = "") {
        breadcrumb(name, details)
    }

    fun captureCrash(thread: Thread, throwable: Throwable) {
        persistReport(thread, throwable)
    }

    private fun breadcrumb(name: String, details: String) {
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        timestampFormat.timeZone = TimeZone.getDefault()
        val normalizedDetails = details.replace(Regex("\\s+"), " ").trim()
        val line = buildString {
            append(timestampFormat.format(Date()))
            append(" | ")
            append(currentScreen)
            append(" | ")
            append(name)
            if (normalizedDetails.isNotEmpty()) {
                append(" | ")
                append(normalizedDetails.take(220))
            }
        }

        synchronized(lock) {
            breadcrumbs.addLast(line)
            while (breadcrumbs.size > MAX_BREADCRUMBS) {
                breadcrumbs.removeFirst()
            }
            lastAction = if (normalizedDetails.isBlank()) name else "$name: $normalizedDetails"
        }
        writeTrace(line)
    }

    private fun writeTrace(line: String) {
        writableRoots().forEach { directory ->
            runCatching {
                val file = File(directory, TRACE_FILE)
                file.parentFile?.mkdirs()
                trimFile(file, MAX_TRACE_BYTES, TRIM_KEEP_BYTES)
                file.appendText("$line\n")
            }
        }
    }

    private fun persistReport(thread: Thread, throwable: Throwable) {
        val context = appContext ?: return
        val report = buildReport(context, thread, throwable)
        writableRoots().forEach { directory ->
            runCatching {
                directory.mkdirs()
                writeText(File(directory, REPORT_FILE), report)
                val historyFile = File(directory, HISTORY_FILE)
                trimFile(historyFile, MAX_HISTORY_BYTES, TRIM_KEEP_BYTES)
                historyFile.appendText("$report\n\n${"=".repeat(80)}\n\n")
            }
        }
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        @Suppress("DEPRECATION")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: -1L
        } else {
            (packageInfo?.versionCode ?: -1).toLong()
        }
        val runtime = Runtime.getRuntime()

        return buildString {
            appendLine("AtsuLab Crash Report")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())}")
            appendLine("App uptime: ${formatUptime(SystemClock.elapsedRealtime() - processStartElapsedRealtime)}")
            appendLine("Screen: $currentScreen")
            appendLine("Last action: $lastAction")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine("Package: ${context.packageName}")
            appendLine("Version: ${packageInfo?.versionName ?: "unknown"} ($versionCode)")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Patch level: ${Build.VERSION.SECURITY_PATCH}")
            appendLine("Process start: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(processStartTimeMillis()))}")
            appendLine(
                "Memory used/free/max: ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024} MB / " +
                    "${runtime.freeMemory() / 1024 / 1024} MB / ${runtime.maxMemory() / 1024 / 1024} MB"
            )
            appendLine("Internal path: ${File(context.filesDir, REPORT_FILE).absolutePath}")
            context.getExternalFilesDir(null)?.let {
                appendLine("External path: ${File(it, REPORT_FILE).absolutePath}")
            }
            appendLine()
            appendLine(throwable.stackTraceToString())
            appendLine()
            appendLine("Breadcrumbs:")
            synchronized(lock) {
                for (breadcrumb in breadcrumbs) {
                    appendLine(breadcrumb)
                }
            }
        }
    }

    private fun processStartTimeMillis(): Long {
        val startUptime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Process.getStartElapsedRealtime()
        } else {
            processStartElapsedRealtime
        }
        return System.currentTimeMillis() - (SystemClock.elapsedRealtime() - startUptime)
    }

    private fun formatUptime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02dh %02dm %02ds".format(Locale.US, hours, minutes, seconds)
    }

    private fun writableRoots(): List<File> {
        val context = appContext ?: return emptyList()
        return listOfNotNull(
            context.filesDir,
            runCatching { context.getExternalFilesDir(null) }.getOrNull()
        )
    }

    private fun writeText(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    private fun trimFile(file: File, maximumBytes: Long, keepBytes: Long) {
        if (!file.isFile || file.length() <= maximumBytes) return
        val content = file.readText()
        file.writeText(content.takeLast(keepBytes.toInt()))
    }

    private class CrashReporterHandler(
        private val previousHandler: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching { captureCrash(thread, throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
