package msr.atsulab.app.player.ui

import msr.atsulab.app.R
import msr.atsulab.app.player.domain.model.SkipInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerSkipControllerTest {

    @Test
    fun `active interval includes start and excludes final second`() {
        val intervals = listOf(SkipInterval(startMs = 1_000L, endMs = 11_000L))

        assertEquals(1_000L, PlayerSkipController.activeInterval(intervals, 1_000L)?.startMs)
        assertEquals(1_000L, PlayerSkipController.activeInterval(intervals, 9_999L)?.startMs)
        assertEquals(null, PlayerSkipController.activeInterval(intervals, 10_000L))
        assertEquals(null, PlayerSkipController.activeInterval(intervals, 11_000L))
    }

    @Test
    fun `uses intro and outro titles by interval type`() {
        assertEquals(
            R.string.player_skip_intro,
            PlayerSkipController.titleResource(SkipInterval(0L, 1_000L, "op"))
        )
        assertEquals(
            R.string.player_skip_outro,
            PlayerSkipController.titleResource(SkipInterval(0L, 1_000L, "ending"))
        )
    }
}
