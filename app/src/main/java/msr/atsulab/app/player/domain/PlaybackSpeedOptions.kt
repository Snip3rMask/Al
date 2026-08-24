package msr.atsulab.app.player.domain

import kotlin.math.abs

internal object PlaybackSpeedOptions {
    const val DEFAULT = 1f
    val VALUES = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun isSelected(currentSpeed: Float, targetSpeed: Float): Boolean {
        return abs(currentSpeed - targetSpeed) < 0.02f
    }

    fun normalize(speed: Float): Float {
        return VALUES.firstOrNull { isSelected(speed, it) } ?: DEFAULT
    }

    fun label(speed: Float): String = "${speed}x"
}
