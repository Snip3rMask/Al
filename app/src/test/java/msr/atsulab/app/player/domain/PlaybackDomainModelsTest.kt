package msr.atsulab.app.player.domain

import msr.atsulab.app.player.domain.model.PlaybackProgress
import msr.atsulab.app.player.domain.model.SkipInterval
import msr.atsulab.app.player.domain.model.VideoSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackDomainModelsTest {

    @Test
    fun `video source preserves anifux defaults`() {
        val videoSource = VideoSource(quality = "1080p", url = "https://example.com/stream.m3u8")

        assertEquals("1080p", videoSource.language)
        assertEquals("Server S1", videoSource.server)
        assertEquals("primary", videoSource.legacySourceId)
    }

    @Test
    fun `skip interval detects ending variants without changing unknown types`() {
        val ending = SkipInterval(startMs = 1_000L, endMs = 2_000L, type = "Ending")
        val opening = SkipInterval(startMs = 0L, endMs = 500L, type = "custom-op")

        assertTrue(ending.isEnding)
        assertFalse(opening.isEnding)
        assertEquals("custom-op", opening.type)
    }

    @Test
    fun `playback progress clamps percentage and applies watched threshold`() {
        val partial = createProgress(positionMs = 4_750L, durationMs = 10_000L)
        val completed = createProgress(positionMs = 9_600L, durationMs = 10_000L)
        val invalid = createProgress(positionMs = -10_000L, durationMs = 10_000L)

        assertEquals(47, partial.percent)
        assertFalse(partial.isConsideredWatched())
        assertTrue(completed.isConsideredWatched())
        assertEquals(PlaybackProgress.MIN_PERCENT, invalid.percent)
    }

    private fun createProgress(positionMs: Long, durationMs: Long): PlaybackProgress {
        return PlaybackProgress(
            aniListId = 21,
            playbackId = "playback-id",
            episodeUrl = "https://example.com/episode",
            animeTitle = "AtsuLab Anime",
            thumbnailImageUrl = "",
            bannerImageUrl = "",
            episodeName = "Episode 1",
            episodeNumber = 1f,
            sourceId = "primary",
            sourceDisplayName = "Daki",
            quality = "1080p",
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAtEpochMs = 1_000L
        )
    }
}
