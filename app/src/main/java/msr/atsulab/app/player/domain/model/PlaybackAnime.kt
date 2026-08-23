package msr.atsulab.app.player.domain.model

data class PlaybackAnime(
    val aniListId: Int,
    val malId: Int? = null,
    val title: String,
    val alternativeTitles: List<String> = emptyList(),
    val coverImageUrl: String = "",
    val bannerImageUrl: String = "",
    val totalEpisodes: Int? = null,
    val episodeDurationMinutes: Int? = null,
    val releaseYear: Int? = null,
    val releaseStatus: ReleaseStatus = ReleaseStatus.UNKNOWN,
    val isAdult: Boolean = false,
    val countryOfOrigin: String? = null,
    val externalIds: Map<String, String> = emptyMap()
) {
    enum class ReleaseStatus {
        FINISHED,
        RELEASING,
        NOT_YET_RELEASED,
        CANCELLED,
        HIATUS,
        UNKNOWN
    }
}
