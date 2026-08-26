package msr.atsulab.app.ui.home

import msr.atsulab.app.helper.pojo.HomeItem
import msr.atsulab.app.player.domain.model.PlaybackProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeContinueWatchingReducerTest {

    @Test
    fun `updates continue watching section without changing home order`() {
        val items = listOf(
            HomeItem(viewType = HomeItem.VIEW_TYPE_HEADER),
            HomeItem(viewType = HomeItem.VIEW_TYPE_CONTINUE_WATCHING),
            HomeItem(viewType = HomeItem.VIEW_TYPE_TRENDING_ANIME)
        )
        val entry = progress()

        val updated = applyContinueWatching(items, listOf(entry))

        assertEquals(3, updated.size)
        assertEquals(listOf(entry), updated[1].continueWatching)
        assertTrue(updated[0].continueWatching.isEmpty())
        assertTrue(updated[2].continueWatching.isEmpty())
    }

    @Test
    fun `returns original list when section is unavailable`() {
        val items = listOf(
            HomeItem(viewType = HomeItem.VIEW_TYPE_HEADER),
            HomeItem(viewType = HomeItem.VIEW_TYPE_MENU)
        )
        val updated = applyContinueWatching(items, listOf(progress()))

        assertSame(items, updated)
    }

    private fun progress(): PlaybackProgress {
        return PlaybackProgress(
            aniListId = 21,
            playbackId = "episode-1",
            episodeUrl = "https://example.com/episode-1",
            animeTitle = "AtsuLab Anime",
            thumbnailImageUrl = "cover.jpg",
            bannerImageUrl = "",
            episodeName = "Episode 1",
            episodeNumber = 1f,
            sourceId = "primary",
            sourceDisplayName = "Server S1",
            quality = "1080p",
            positionMs = 4_000L,
            durationMs = 10_000L,
            updatedAtEpochMs = 1_000L
        )
    }
}
