package msr.atsulab.app.player.runtime

import msr.atsulab.app.player.domain.model.PlaybackEpisode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlaybackEpisodeNavigatorTest {

    @Test
    fun `reset selects exact episode number before positional fallback`() {
        val navigator = PlaybackEpisodeNavigator()
        val special = PlaybackEpisode(name = "Special", url = "special", number = 0f)
        val second = PlaybackEpisode(name = "Second", url = "second", number = 2f)

        navigator.reset(listOf(special, second), requestedNumber = 2)

        assertEquals(second, navigator.currentEpisode)
        assertEquals(1, navigator.selectedIndex)
    }

    @Test
    fun `reset falls back to requested list position`() {
        val navigator = PlaybackEpisodeNavigator()
        val first = PlaybackEpisode(name = "First", url = "first", number = 1.5f)
        val second = PlaybackEpisode(name = "Second", url = "second", number = 2.5f)

        navigator.reset(listOf(first, second), requestedNumber = 2)

        assertEquals(second, navigator.currentEpisode)
    }

    @Test
    fun `move returns adjacent episodes without wrapping`() {
        val navigator = PlaybackEpisodeNavigator()
        val first = PlaybackEpisode(name = "First", url = "first")
        val second = PlaybackEpisode(name = "Second", url = "second")
        val third = PlaybackEpisode(name = "Third", url = "third")
        navigator.reset(listOf(first, second, third), requestedNumber = 2)

        assertEquals(first, navigator.move(-1))
        assertEquals(0, navigator.selectedIndex)
        assertNull(navigator.move(-1))

        assertEquals(second, navigator.move(1))
        assertEquals(third, navigator.move(1))
        assertEquals(2, navigator.selectedIndex)
        assertNull(navigator.move(1))
    }

    @Test
    fun `availability reports adjacent movement without wrapping`() {
        val navigator = PlaybackEpisodeNavigator()
        val first = PlaybackEpisode(name = "First", url = "first", number = 1f)
        val second = PlaybackEpisode(name = "Second", url = "second", number = 2f)
        val third = PlaybackEpisode(name = "Third", url = "third", number = 3f)

        assertEquals(false, navigator.canMove(-1))
        assertEquals(false, navigator.canMove(1))

        navigator.reset(listOf(first, second, third), requestedNumber = 1)

        assertEquals(false, navigator.canMove(-1))
        assertEquals(true, navigator.canMove(1))
        navigator.move(1)

        assertEquals(true, navigator.canMove(-1))
        assertEquals(true, navigator.canMove(1))
        assertEquals(false, navigator.canMove(0))
        navigator.move(1)

        assertEquals(true, navigator.canMove(-1))
        assertEquals(false, navigator.canMove(1))
    }

    @Test
    fun `move is unavailable without a valid selection`() {
        val navigator = PlaybackEpisodeNavigator()

        assertNull(navigator.move(1))
        assertNull(navigator.currentEpisode)
        assertEquals(-1, navigator.selectedIndex)
    }

    @Test
    fun `reset reports invalid requests and blocks movement`() {
        val navigator = PlaybackEpisodeNavigator()
        val first = PlaybackEpisode(name = "First", url = "first")

        assertNull(navigator.reset(listOf(first), requestedNumber = 2))
        assertNull(navigator.move(1))
    }
}

    @Test
    fun `select chooses matching url and number then updates movement`() {
        val navigator = PlaybackEpisodeNavigator()
        val first = PlaybackEpisode(name = "First", url = "first", number = 1f)
        val second = PlaybackEpisode(name = "Second", url = "second", number = 2f)
        val third = PlaybackEpisode(name = "Third", url = "third", number = 3f)
        navigator.reset(listOf(first, second, third), requestedNumber = 1)

        assertEquals(second, navigator.select(second.copy(name = "Selected")))
        assertEquals(1, navigator.selectedIndex)
        assertEquals(third, navigator.move(1))
    }

    @Test
    fun `select rejects unknown episode without changing selection`() {
        val navigator = PlaybackEpisodeNavigator()
        val first = PlaybackEpisode(name = "First", url = "first", number = 1f)
        navigator.reset(listOf(first), requestedNumber = 1)

        assertEquals(null, navigator.select(PlaybackEpisode(name = "Unknown", url = "unknown")))
        assertEquals(first, navigator.currentEpisode)
    }
