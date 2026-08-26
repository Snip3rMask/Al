package msr.atsulab.app.ui.media

import msr.atsulab.app.player.domain.model.PlaybackProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MediaDetailsResumeProgressTest {

    @Test
    fun `selects latest unfinished episode for the current anime`() {
        val selected = progress(updatedAtEpochMs = 30L, episodeNumber = 3f, positionMs = 4_000L)
        val entries = listOf(
            progress(updatedAtEpochMs = 10L, episodeNumber = 1f, aniListId = 99),
            progress(updatedAtEpochMs = 20L, episodeNumber = 2f, positionMs = 0L),
            selected,
            progress(updatedAtEpochMs = 40L, episodeNumber = 4f, positionMs = 9_600L)
        )

        assertEquals(selected, selectResumableProgress(21, entries))
    }

    @Test
    fun `returns null without a matching resumable entry`() {
        assertNull(selectResumableProgress(21, emptyList()))
        assertNull(selectResumableProgress(21, listOf(progress(positionMs = 0L))))
        assertNull(selectResumableProgress(21, listOf(progress(positionMs = 9_600L))))
    }

    @Test
    fun `episode labels hide trailing zero`() {
        assertEquals("3", formatPlaybackEpisodeNumber(3f))
        assertEquals("3.5", formatPlaybackEpisodeNumber(3.5f))
    }

    private fun progress(
        positionMs: Long = 4_000L,
        updatedAtEpochMs: Long = 1_000L,
        episodeNumber: Float = 1f,
        aniListId: Int? = 21
    ): PlaybackProgress {
        return PlaybackProgress(
            aniListId = aniListId,
            playbackId = "episode-$episodeNumber",
            episodeUrl = "https://example.com/$episodeNumber",
            animeTitle = "AtsuLab Anime",
            thumbnailImageUrl = "",
            bannerImageUrl = "",
            episodeName = "Episode $episodeNumber",
            episodeNumber = episodeNumber,
            sourceId = "primary",
            sourceDisplayName = "Server S1",
            quality = "1080p",
            positionMs = positionMs,
            durationMs = 10_000L,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }
}
