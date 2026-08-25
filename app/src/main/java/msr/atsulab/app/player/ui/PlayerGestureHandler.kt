package msr.atsulab.app.player.ui

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

internal class PlayerGestureHandler(
    context: Context,
    private val callbacks: Callbacks
) : View.OnTouchListener {

    private val activationThresholdPx = context.resources.displayMetrics.density *
        PlayerShellMetrics.VERTICAL_GESTURE_ACTIVATION_DP
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var startBrightness = 0f
    private var startVolume = 0
    private var isAdjusting = false

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                callbacks.onSingleTap()
                return true
            }

            override fun onDoubleTap(event: MotionEvent): Boolean {
                val half = context.resources.displayMetrics.widthPixels / 2f
                callbacks.onSeek(event.x >= half)
                return true
            }
        }
    )

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (callbacks.isControlsLocked()) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                callbacks.onLockedTouch()
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                startBrightness = callbacks.currentBrightness()
                startVolume = callbacks.currentVolume()
                isAdjusting = false
                callbacks.onPlaybackTouchStarted()
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - touchDownX
                val deltaY = event.y - touchDownY
                if (!isAdjusting &&
                    kotlin.math.abs(deltaY) > activationThresholdPx &&
                    kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * DIAGONAL_BIAS_RATIO
                ) {
                    isAdjusting = true
                }

                if (isAdjusting) {
                    if (view.resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    ) {
                        val normalizedDelta = -deltaY / view.height.coerceAtLeast(1)
                        if (touchDownX < view.width / 2f) {
                            adjustBrightness(normalizedDelta)
                        } else {
                            adjustVolume(normalizedDelta)
                        }
                    }
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isAdjusting) {
                    isAdjusting = false
                    callbacks.hideHudSoon()
                    return true
                }
            }
        }

        return detector.onTouchEvent(event)
    }

    private fun adjustBrightness(delta: Float) {
        val brightness = clampValue(startBrightness + delta * BRIGHTNESS_SENSITIVITY, MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        callbacks.setBrightness(brightness)
        callbacks.showHud(HUD_LABEL_BRIGHTNESS, brightness, isRightSide = true)
    }

    private fun adjustVolume(delta: Float) {
        val maxVolume = callbacks.maxVolume().coerceAtLeast(1)
        val volume = (startVolume + kotlin.math.round(delta * maxVolume * VOLUME_SENSITIVITY).toInt())
            .coerceIn(0, maxVolume)
        callbacks.setVolume(volume)
        callbacks.showHud(HUD_LABEL_VOLUME, volume / maxVolume.toFloat(), isRightSide = false)
    }

    private fun clampValue(value: Float, minimum: Float, maximum: Float): Float {
        return value.coerceIn(minimum, maximum)
    }

    internal interface Callbacks {
        fun isControlsLocked(): Boolean
        fun onLockedTouch()
        fun onPlaybackTouchStarted()
        fun onSingleTap()
        fun onSeek(isForward: Boolean)
        fun currentBrightness(): Float
        fun setBrightness(value: Float)
        fun currentVolume(): Int
        fun maxVolume(): Int
        fun setVolume(value: Int)
        fun showHud(label: String, level: Float, isRightSide: Boolean)
        fun hideHudSoon()
    }

    private companion object {
        const val HUD_LABEL_BRIGHTNESS = "Brightness"
        const val HUD_LABEL_VOLUME = "Volume"
        const val DIAGONAL_BIAS_RATIO = 1.15f
        const val BRIGHTNESS_SENSITIVITY = 1.25f
        const val VOLUME_SENSITIVITY = 1.25f
        const val MIN_BRIGHTNESS = 0.05f
        const val MAX_BRIGHTNESS = 1f
    }
}
