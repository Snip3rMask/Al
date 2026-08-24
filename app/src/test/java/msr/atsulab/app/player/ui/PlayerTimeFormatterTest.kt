package msr.atsulab.app.player.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerTimeFormatterTest {

    @Test
    fun `formats elapsed and duration times as minutes and seconds`() {
        assertEquals("00:00", PlayerTimeFormatter.format(0L))
        assertEquals("00:01", PlayerTimeFormatter.format(1_000L))
        assertEquals("00:59", PlayerTimeFormatter.format(59_999L))
        assertEquals("01:00", PlayerTimeFormatter.format(60_000L))
        assertEquals("19:39", PlayerTimeFormatter.format(1_179_500L))
    }

    @Test
    fun `keeps anifux minute rollover format for one hour`() {
        assertEquals("60:00", PlayerTimeFormatter.format(3_600_000L))
        assertEquals("61:01", PlayerTimeFormatter.format(3_661_000L))
    }

    @Test
    fun `clamps negative time to zero`() {
        assertEquals("00:00", PlayerTimeFormatter.format(-1L))
    }
}
