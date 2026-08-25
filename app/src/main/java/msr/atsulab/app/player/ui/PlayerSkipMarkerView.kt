package msr.atsulab.app.player.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import msr.atsulab.app.player.domain.model.SkipInterval
import msr.atsulab.app.player.ui.PlayerShellMetrics.ACCENT_COLOR

internal class PlayerSkipMarkerView(context: Context) : View(context) {

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ACCENT_COLOR
        alpha = 190
    }

    private var intervals: List<SkipInterval> = emptyList()
    private var durationMs = 0L

    fun setData(intervals: List<SkipInterval>, durationMs: Long) {
        this.intervals = intervals.toList()
        this.durationMs = durationMs
        visibility = if (this.intervals.isEmpty() || durationMs <= 0L) GONE else VISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (durationMs <= 0L || intervals.isEmpty()) return

        val markerHeight = PlayerShellMetrics.SKIP_MARKER_HEIGHT_DP * resources.displayMetrics.density
        val top = (height - markerHeight) / 2f
        intervals.forEach { interval ->
            val leftFraction = (interval.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val rightFraction = (interval.endMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val left = width * leftFraction
            val right = width * rightFraction
            if (right > left) {
                canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    top + markerHeight,
                    markerHeight / 2f,
                    markerHeight / 2f,
                    markerPaint
                )
            }
        }
    }
}
