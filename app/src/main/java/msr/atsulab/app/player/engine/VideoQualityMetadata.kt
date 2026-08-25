package msr.atsulab.app.player.engine

import msr.atsulab.app.player.domain.model.VideoQuality

internal object VideoQualityMetadata {

    fun create(
        id: String,
        fallbackLabel: String,
        width: Int,
        height: Int,
        bitrate: Long,
        isSelected: Boolean
    ): VideoQuality? {
        val label = displayLabel(height, bitrate, fallbackLabel) ?: return null
        return VideoQuality(
            id = id,
            label = label,
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
            bitrate = bitrate.coerceAtLeast(0L),
            isSelected = isSelected
        )
    }

    fun sorted(qualities: List<VideoQuality>): List<VideoQuality> {
        return qualities.sortedWith(
            compareByDescending<VideoQuality> { it.height }
                .thenByDescending { it.bitrate }
                .thenByDescending { it.width }
                .thenBy(VideoQuality::label)
        )
    }

    fun displayLabel(height: Int, bitrate: Long, fallbackLabel: String): String? {
        if (height > 0) return "${height}p"
        if (bitrate >= FULL_HD_BITRATE) return "1080p"
        if (bitrate >= HD_BITRATE) return "720p"
        if (bitrate >= SD_BITRATE) return "480p"
        if (bitrate > 0L) return "360p"
        return fallbackLabel.trim().takeIf(String::isNotEmpty)
    }

    private const val FULL_HD_BITRATE = 4_500_000L
    private const val HD_BITRATE = 2_200_000L
    private const val SD_BITRATE = 1_000_000L
}
