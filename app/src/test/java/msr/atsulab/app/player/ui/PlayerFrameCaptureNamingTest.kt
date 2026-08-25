package msr.atsulab.app.player.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PlayerFrameCaptureNamingTest {

    @Test
    fun `safe segment removes invalid and empty values`() {
        assertEquals("Frieren _ S2_E01", PlayerFrameCaptureNaming.safeSegment("Frieren / S2:E01?"))
        assertEquals("frame", PlayerFrameCaptureNaming.safeSegment("///"))
    }

    @Test
    fun `safe segment limits length`() {
        val value = PlayerFrameCaptureNaming.safeSegment("a".repeat(100))

        assertEquals(60, value.length)
    }

    @Test
    fun `file name contains sanitized episode and png extension`() {
        val fileName = PlayerFrameCaptureNaming.fileName("Episode 1/?")

        assertTrue(fileName.startsWith("AtsuLab_Episode 1_"))
        assertTrue(fileName.endsWith(".png"))
        assertFalse(fileName.contains('?'))
    }
}
