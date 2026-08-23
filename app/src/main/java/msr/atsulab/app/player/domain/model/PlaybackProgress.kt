package msr.atsulab.app.player.domain.model

data class PlaybackProgress(
    val aniListId: Int?,
    val playbackId: String,
    val episodeUrl: String,
    val animeTitle: String,
    val thumbnailImageUrl: String,
    val bannerImageUrl: String,
    val episodeName: String,
    val episodeNumber: Float,
    val sourceId: String,
    val sourceDisplayName: String,
    val quality: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMs: Long
) {
    val percent: Int
        get() {
            if (durationMs <= 0L) return 0
            return ((positionMs * PERCENT_SCALE) / durationMs)
                .toInt()
                .coerceIn(MIN_PERCENT, MAX_PERCENT)
        }

    fun isConsideredWatched(watchedThresholdPercent: Int = WATCHED_THRESHOLD_PERCENT): Boolean {
        return percent >= watchedThresholdPercent
    }

    companion object {
        const val MIN_PERCENT = 0
        const val MAX_PERCENT = 100
        const val PERCENT_SCALE = 100L
        const val WATCHED_THRESHOLD_PERCENT = 95
    }
}
