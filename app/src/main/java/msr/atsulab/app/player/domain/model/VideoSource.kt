package msr.atsulab.app.player.domain.model

data class VideoSource(
    val quality: String,
    val url: String,
    val language: String = quality,
    val server: String = DEFAULT_SERVER,
    val legacySourceId: String = LEGACY_SOURCE_ID,
    val subtitleUrl: String = "",
    val displayName: String = "",
    val referer: String = "",
    val providerId: String? = null,
    val skipIntervals: List<SkipInterval> = emptyList()
) {
    companion object {
        const val DEFAULT_SERVER = "Server S1"
        const val LEGACY_SOURCE_ID = "primary"
    }
}
