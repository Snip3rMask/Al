package msr.atsulab.app.player.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

internal class PlayerGestureHudView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var label = LABEL_VOLUME
    private var level = 0.5f

    fun setLevel(label: String, level: Float) {
        this.label = label
        this.level = level.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.WHITE
        paint.setShadowLayer(10f, 0f, 2f, HUD_SHADOW_COLOR)

        paint.textSize = width * HUD_LABEL_SIZE_RATIO
        canvas.drawText(
            if (label.startsWith("B")) LABEL_BRIGHTNESS_DISPLAY else LABEL_VOLUME,
            width / 2f,
            height * HUD_LABEL_VERTICAL_RATIO,
            paint
        )

        rect.set(
            width * HUD_TRACK_HORIZONTAL_RATIO,
            height * HUD_TRACK_TOP_RATIO,
            width * (1f - HUD_TRACK_HORIZONTAL_RATIO),
            height * HUD_TRACK_BOTTOM_RATIO
        )
        paint.clearShadowLayer()
        paint.color = HUD_TRACK_COLOR
        canvas.drawRoundRect(rect, width * HUD_CORNER_RATIO, width * HUD_CORNER_RATIO, paint)

        val fillTop = rect.bottom - rect.height() * level
        val fillRect = RectF(rect.left, fillTop, rect.right, rect.bottom)
        paint.color = PlayerShellMetrics.ACCENT_COLOR
        canvas.drawRoundRect(fillRect, width * HUD_CORNER_RATIO, width * HUD_CORNER_RATIO, paint)

        paint.color = Color.WHITE
        paint.setShadowLayer(8f, 0f, 2f, HUD_SHADOW_COLOR)
        paint.textSize = width * HUD_PERCENT_SIZE_RATIO
        canvas.drawText("${kotlin.math.round(level * 100).toInt()}%", width / 2f, height * HUD_VALUE_VERTICAL_RATIO, paint)
    }

    private companion object {
        const val LABEL_VOLUME = "Volume"
        const val LABEL_BRIGHTNESS_DISPLAY = "SUN"
        const val LABEL_VOLUME = "VOL"
        const val HUD_LABEL_SIZE_RATIO = 0.16f
        const val HUD_LABEL_VERTICAL_RATIO = 0.20f
        const val HUD_TRACK_HORIZONTAL_RATIO = 0.46f
        const val HUD_TRACK_TOP_RATIO = 0.30f
        const val HUD_TRACK_BOTTOM_RATIO = 0.75f
        const val HUD_CORNER_RATIO = 0.06f
        const val HUD_TRACK_COLOR = 0x66FFFFFF.toInt()
        const val HUD_SHADOW_COLOR = 0xAA000000.toInt()
        const val HUD_PERCENT_SIZE_RATIO = 0.18f
        const val HUD_VALUE_VERTICAL_RATIO = 0.91f
    }
}
