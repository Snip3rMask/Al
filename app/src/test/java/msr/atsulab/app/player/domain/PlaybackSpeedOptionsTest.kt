package msr.atsulab.app.player.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackSpeedOptionsTest {

    @Test
    fun `freezes anifux speed values`() {
        assertEquals(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f), PlaybackSpeedOptions.VALUES)
        assertEquals(1f, PlaybackSpeedOptions.DEFAULT)
    }

    @Test
    fun `selects speeds within tolerance`() {
        assertTrue(PlaybackSpeedOptions.isSelected(1.0f, 1.01f))
        assertFalse(PlaybackSpeedOptions.isSelected(1.0f, 1.03f))
    }

    @Test
    fun `normalizes persisted values to supported options`() {
        assertEquals(1f, PlaybackSpeedOptions.normalize(-0.2f))
        assertEquals(1.25f, PlaybackSpeedOptions.normalize(1.26f))
        assertEquals(1f, PlaybackSpeedOptions.normalize(9f))
        assertEquals(1f, PlaybackSpeedOptions.normalize(0.9f))
    }

    @Test
    fun `formats speed labels like anifux`() {
        assertEquals("0.75x", PlaybackSpeedOptions.label(0.75f))
        assertEquals("1.0x", PlaybackSpeedOptions.label(1.0f))
        assertEquals("2.0x", PlaybackSpeedOptions.label(2.0f))
    }
}
