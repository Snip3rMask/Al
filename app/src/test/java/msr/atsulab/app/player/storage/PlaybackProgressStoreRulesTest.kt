package msr.atsulab.app.player.storage

import msr.atsulab.app.player.domain.model.PlaybackProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackProgressStoreRulesTest {

    @Test
    fun `progress identity separates anime episode and source entry`() {
        val first = progress(aniListId = 21, playbackId = "episode-1", episodeUrl = "episode-1")
        val sameEpisodeDifferentAnime = first.copy(aniListId = 22)
        val differentEpisode = first.copy(playbackId = "episode-2", episodeUrl = "episode-2")

        assertEquals(playbackProgressKey(first), playbackProgressKey(first.copy(sourceId = "other")))
        assertNotEquals(playbackProgressKey(first), playbackProgressKey(sameEpisodeDifferentAnime))
        assertNotEquals(playbackProgressKey(first), playbackProgressKey(differentEpisode))
    }

    @Test
    fun `rejects invalid negative or impossible progress`() {
        assertTrue(isStorablePlaybackProgress(progress(positionMs = 4_000L, durationMs = 10_000L)))
        assertFalse(isStorablePlaybackProgress(progress(positionMs = -1L, durationMs = 10_000L)))
        assertFalse(isStorablePlaybackProgress(progress(positionMs = 10_001L, durationMs = 10_000L)))
        assertFalse(isStorablePlaybackProgress(progress(positionMs = 0L, durationMs = 0L)))
        assertFalse(isStorablePlaybackProgress(progress(positionMs = 1L, durationMs = 10_000L).copy(playbackId = " ")))
    }

    @Test
    fun `completed threshold marks near finished episodes watched`() {
        val progress = progress(positionMs = 9_600L, durationMs = 10_000L)

        assertTrue(progress.isConsideredWatched())
        assertEquals(PlaybackProgress.MAX_PERCENT, progress.percent)
    }

    private fun progress(
        positionMs: Long,
        durationMs: Long,
        aniListId: Int? = 21,
        playbackId: String = "episode-1",
        episodeUrl: String = "episode-1"
    ): PlaybackProgress {
        return PlaybackProgress(
            aniListId = aniListId,
            playbackId = playbackId,
            episodeUrl = episodeUrl,
            animeTitle = "AtsuLab Anime",
            thumbnailImageUrl = "",
            bannerImageUrl = "",
            episodeName = "Episode 1",
            episodeNumber = 1f,
            sourceId = "primary",
            sourceDisplayName = "Server S1",
            quality = "1080p",
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAtEpochMs = 1_000L
        )
    }
}
