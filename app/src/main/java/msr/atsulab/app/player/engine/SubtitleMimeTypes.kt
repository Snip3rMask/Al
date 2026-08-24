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

internal object SubtitleTrackMetadata {

    fun displayLabel(label: String?, language: String?): String? {
        val normalizedLanguage = language?.takeIf(String::isNotBlank)?.lowercase()
        val normalizedLabel = label?.takeIf(String::isNotBlank)
        if (normalizedLanguage == null) return normalizedLabel
        if (normalizedLabel == null || normalizedLanguage.contains(normalizedLabel)) {
            return normalizedLanguage
        }
        return "$normalizedLabel ($normalizedLanguage)"
    }
}
