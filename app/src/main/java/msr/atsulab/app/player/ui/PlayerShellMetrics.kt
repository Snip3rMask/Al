package msr.atsulab.app.player.ui

import kotlin.math.max
import kotlin.math.roundToInt

object PlayerShellMetrics {
    val PRIMARY_DARK_COLOR = 0xFF0A0A0D.toInt()
    val SURFACE_COLOR = 0xFF1F222A.toInt()
    val ACCENT_COLOR = 0xFF0EA5E9.toInt()

    const val WATCHING_TEXT_SIZE_SP = 18
    const val SOURCE_ERROR_TEXT_SIZE_SP = 14
    const val LANDSCAPE_TITLE_TEXT_SIZE_SP = 20
    const val PORTRAIT_TITLE_TEXT_SIZE_SP = 26

    const val LANDSCAPE_TOP_HEIGHT_DP = 86
    const val PORTRAIT_TOP_HEIGHT_DP = 102
    const val BOTTOM_CONTROLS_HEIGHT_DP = 112
    const val PROGRESS_ROW_HEIGHT_DP = 38
    const val SEEK_CONTROL_HEIGHT_DP = 32
    const val CONTROL_ICON_SIZE_DP = 44
    const val BACK_BUTTON_SIZE_DP = 42
    const val TRANSPORT_ROW_HEIGHT_DP = 58
    const val TRANSPORT_ICON_OFFSET_DP = 70
    const val LOCK_BUTTON_LEFT_MARGIN_DP = 24
    const val UNLOCK_BUTTON_LEFT_MARGIN_DP = 48
    const val UNLOCK_BUTTON_PORTRAIT_TOP_MARGIN_DP = 138
    const val UNLOCK_BUTTON_LANDSCAPE_TOP_MARGIN_DP = 92
    const val LOADING_INDICATOR_SIZE_DP = 58
    const val WATCHING_ROW_HEIGHT_DP = 72
    const val EPISODE_GRID_COLUMN_COUNT = 5

    const val PORTRAIT_VIDEO_MIN_HEIGHT_PX = 460
    const val PORTRAIT_VIDEO_SCREEN_RATIO = 0.30f

    fun portraitVideoHeightPixels(screenHeightPixels: Int): Int {
        require(screenHeightPixels >= 0) { "Screen height must not be negative" }
        return max(
            PORTRAIT_VIDEO_MIN_HEIGHT_PX,
            (screenHeightPixels * PORTRAIT_VIDEO_SCREEN_RATIO).roundToInt()
        )
    }
}
