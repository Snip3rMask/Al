package msr.atsulab.app.player.engine

import androidx.media3.common.MimeTypes

internal object SubtitleMimeTypes {

    fun fromUrl(url: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
            else -> MimeTypes.TEXT_VTT
        }
    }
}
