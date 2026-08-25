package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.VideoQuality

internal data class VideoQualityOption(
    val id: String?,
    val label: String,
    val isSelected: Boolean
)

internal object PlayerQualityMenuModel {

    fun options(
        qualities: List<VideoQuality>,
        selectedTrackId: String?
    ): List<VideoQualityOption> {
        return listOf(
            VideoQualityOption(
                id = null,
                label = "AUTO",
                isSelected = selectedTrackId == null
            )
        ) + qualities.map { quality ->
            VideoQualityOption(
                id = quality.id,
                label = quality.label,
                isSelected = quality.id == selectedTrackId
            )
        }
    }

    fun controlLabel(
        qualities: List<VideoQuality>,
        selectedTrackId: String?
    ): String {
        if (selectedTrackId == null) return "AUTO"
        val label = qualities.firstOrNull { it.id == selectedTrackId }?.label.orEmpty()
            .trim()
            .uppercase()
        if (label.isEmpty()) return "AUTO"

        val number = Regex("(\\d{3,4})").find(label)?.groupValues?.lastOrNull()?.toIntOrNull() ?: 0
        return when {
            number >= 1080 -> "FHD"
            number >= 720 -> "HD"
            number > 0 -> "SD"
            else -> label
        }
    }
}
