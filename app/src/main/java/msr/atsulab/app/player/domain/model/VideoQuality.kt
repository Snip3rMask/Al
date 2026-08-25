package msr.atsulab.app.player.domain.model

data class VideoQuality(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Long,
    val isSelected: Boolean
)
