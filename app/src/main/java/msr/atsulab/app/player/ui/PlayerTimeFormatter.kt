package msr.atsulab.app.player.ui

import java.util.Locale

internal object PlayerTimeFormatter {

    fun format(timeMs: Long): String {
        val totalSeconds = timeMs.coerceAtLeast(0L) / 1_000L
        return String.format(
            Locale.US,
            "%02d:%02d",
            totalSeconds / 60L,
            totalSeconds % 60L
        )
    }
}
