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

    fun persist(context: Context, thread: Thread, throwable: Throwable): File? {
        return writeReport(context, buildReport(thread, throwable))
    }

    fun read(context: Context): String? {
        return reportFiles(context)
            .filter { it.isFile && it.length() > 0L }
            .maxByOrNull { it.lastModified() }
            ?.readText()
    }

    fun clear(context: Context) {
        reportFiles(context).forEach(File::delete)
    }

    fun reportPaths(context: Context): List<String> {
        return reportFiles(context).map(File::absolutePath)
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

    private fun writeReport(context: Context, report: String): File? {
        val appContext = context.applicationContext
        val directories = listOfNotNull(
            appContext.filesDir,
            appContext.getExternalFilesDir(null),
            appContext.getExternalFilesDirs(null).firstOrNull { it != appContext.getExternalFilesDir(null) }
        )

        return directories.mapNotNull { directory ->
            runCatching { writeFile(File(directory, FILE_NAME), report) }.getOrNull()
        }.maxByOrNull { it.lastModified() }
    }

    private fun writeFile(file: File, report: String): File {
        file.parentFile?.mkdirs()
        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        temporaryFile.writeText(report)
        if (file.exists()) file.delete()
        if (!temporaryFile.renameTo(file)) {
            file.writeText(report)
            temporaryFile.delete()
        }
        return file
    }

    private fun reportFiles(context: Context): List<File> {
        val appContext = context.applicationContext
        return sequenceOf(appContext.filesDir, *appContext.getExternalFilesDirs(null))
            .filterNotNull()
            .map { File(it, FILE_NAME) }
            .distinct()
            .toList()
    }

    private const val FILE_NAME = "atsu_player_debug_crash.txt"
}
