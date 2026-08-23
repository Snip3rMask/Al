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

    @Synchronized
    fun breadcrumb(context: Context, stage: String) {
        val line = "${SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())} $stage"
        writableRoots(context).forEach { directory ->
            runCatching {
                val file = File(directory, TRACE_FILE)
                file.parentFile?.mkdirs()
                file.appendText(line + "\n")
            }
        }
    }

    fun persist(context: Context, thread: Thread, throwable: Throwable): File? {
        val report = buildReport(thread, throwable)
        val files = writableRoots(context).mapNotNull { directory ->
            runCatching { writeText(File(directory, REPORT_FILE), report) }.getOrNull()
        }
        return files.maxByOrNull { it.lastModified() }
    }

    fun readReport(context: Context): String? {
        return reportFiles(context)
            .filter { it.isFile && it.length() > 0L }
            .maxByOrNull { it.lastModified() }
            ?.readText()
    }

    fun readTrace(context: Context): String? {
        return reportFiles(context)
            .filter { it.name == TRACE_FILE && it.isFile }
            .maxByOrNull { it.lastModified() }
            ?.readText()
    }

    fun clear(context: Context) {
        reportFiles(context).forEach(File::delete)
    }

    fun storagePaths(context: Context): List<String> {
        return writableRoots(context).map { directory ->
            File(directory, REPORT_FILE).absolutePath
        }
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

    private fun writeText(file: File, text: String): File {
        file.parentFile?.mkdirs()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(text)
        if (file.exists()) file.delete()
        if (!temporaryFile.renameTo(file)) {
            file.writeText(text)
            temporaryFile.delete()
        }
        return file
    }

    private fun writableRoots(context: Context): List<File> {
        val appContext = context.applicationContext
        return listOfNotNull(appContext.filesDir, appContext.getExternalFilesDir(null))
    }

    private fun reportFiles(context: Context): List<File> {
        return writableRoots(context).map { directory ->
            File(directory, REPORT_FILE)
        } + writableRoots(context).map { directory ->
            File(directory, TRACE_FILE)
        }
    }

    private const val REPORT_FILE = "atsu_player_debug_crash.txt"
    private const val TRACE_FILE = "atsu_player_debug_trace.txt"
}
