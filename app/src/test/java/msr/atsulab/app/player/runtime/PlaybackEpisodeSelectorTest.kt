package msr.atsulab.app.player.runtime

import msr.atsulab.app.player.domain.model.PlaybackEpisode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlaybackEpisodeSelectorTest {

    @Test
    fun `selects an exact episode number before positional fallback`() {
        val episodes = listOf(
            PlaybackEpisode(name = "Special", url = "special", number = 0f),
            PlaybackEpisode(name = "Episode 2", url = "two", number = 2f),
            PlaybackEpisode(name = "Episode 3", url = "three", number = 3f)
        )

        assertEquals("two", episodes.selectPlaybackEpisode(2)?.url)
    }

    @Test
    fun `falls back to requested list position when numbers are incompatible`() {
        val episodes = listOf(
            PlaybackEpisode(name = "First", url = "first", number = 1.5f),
            PlaybackEpisode(name = "Second", url = "second", number = 2.5f)
        )

        assertEquals("second", episodes.selectPlaybackEpisode(2)?.url)
    }

    @Test
    fun `reports unavailable when request cannot be resolved`() {
        val episodes = listOf(
            PlaybackEpisode(name = "First", url = "first", number = 1f)
        )

        assertNull(episodes.selectPlaybackEpisode(2))
        assertNull(emptyList<PlaybackEpisode>().selectPlaybackEpisode(1))
    }
}
