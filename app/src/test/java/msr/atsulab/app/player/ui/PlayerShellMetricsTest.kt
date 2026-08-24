package msr.atsulab.app.player.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PlayerShellMetricsTest {

    @Test
    fun `freezes anifux shell colors`() {
        assertEquals(0xFF0A0A0D.toInt(), PlayerShellMetrics.PRIMARY_DARK_COLOR)
        assertEquals(0xFF1F222A.toInt(), PlayerShellMetrics.SURFACE_COLOR)
        assertEquals(0xFF0EA5E9.toInt(), PlayerShellMetrics.ACCENT_COLOR)
    }

    @Test
    fun `freezes anifux shell dimensions`() {
        assertEquals(102, PlayerShellMetrics.PORTRAIT_TOP_HEIGHT_DP)
        assertEquals(86, PlayerShellMetrics.LANDSCAPE_TOP_HEIGHT_DP)
        assertEquals(112, PlayerShellMetrics.BOTTOM_CONTROLS_HEIGHT_DP)
        assertEquals(38, PlayerShellMetrics.PROGRESS_ROW_HEIGHT_DP)
        assertEquals(32, PlayerShellMetrics.SEEK_CONTROL_HEIGHT_DP)
        assertEquals(44, PlayerShellMetrics.CONTROL_ICON_SIZE_DP)
        assertEquals(58, PlayerShellMetrics.LOADING_INDICATOR_SIZE_DP)
        assertEquals(72, PlayerShellMetrics.WATCHING_ROW_HEIGHT_DP)
        assertEquals(58, PlayerShellMetrics.TRANSPORT_ROW_HEIGHT_DP)
        assertEquals(70, PlayerShellMetrics.TRANSPORT_ICON_OFFSET_DP)
        assertEquals(24, PlayerShellMetrics.LOCK_BUTTON_LEFT_MARGIN_DP)
        assertEquals(48, PlayerShellMetrics.UNLOCK_BUTTON_LEFT_MARGIN_DP)
        assertEquals(138, PlayerShellMetrics.UNLOCK_BUTTON_PORTRAIT_TOP_MARGIN_DP)
        assertEquals(92, PlayerShellMetrics.UNLOCK_BUTTON_LANDSCAPE_TOP_MARGIN_DP)
        assertEquals(5, PlayerShellMetrics.EPISODE_GRID_COLUMN_COUNT)
    }

    @Test
    fun `calculates portrait video height from thirty percent of screen`() {
        assertEquals(720, PlayerShellMetrics.portraitVideoHeightPixels(2400))
    }

    @Test
    fun `enforces minimum portrait video height`() {
        assertEquals(460, PlayerShellMetrics.portraitVideoHeightPixels(0))
        assertEquals(460, PlayerShellMetrics.portraitVideoHeightPixels(1000))
        assertEquals(460, PlayerShellMetrics.portraitVideoHeightPixels(1534))
        assertEquals(461, PlayerShellMetrics.portraitVideoHeightPixels(1535))
    }

    @Test
    fun `rejects negative screen height`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlayerShellMetrics.portraitVideoHeightPixels(-1)
        }
    }
}
