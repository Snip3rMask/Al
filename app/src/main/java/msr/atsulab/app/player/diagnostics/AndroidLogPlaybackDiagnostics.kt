package msr.atsulab.app.player.diagnostics

import android.util.Log
import msr.atsulab.app.BuildConfig

class AndroidLogPlaybackDiagnostics(
    private val enabled: Boolean = BuildConfig.DEBUG
) : PlaybackDiagnostics by DefaultPlaybackDiagnostics(enabled, ::writeLog)

private fun writeLog(priority: Int, tag: String, message: String, error: Throwable?) {
    val output = if (error == null) {
        message
    } else {
        "$message\n${Log.getStackTraceString(error)}"
    }
    Log.println(priority, tag, output)
}
