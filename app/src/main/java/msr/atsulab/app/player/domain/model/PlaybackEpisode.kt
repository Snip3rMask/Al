package msr.atsulab.app.player.domain.model

data class PlaybackEpisode(
    val name: String,
    val url: String,
    val thumbnailUrl: String = "",
    val number: Float = 0f,
    val postTitle: String = "",
    val playbackId: String = "",
    val confirmedSourceSlug: String? = null,
    val aniListId: Int? = null
)
