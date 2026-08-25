package msr.atsulab.app.player.domain

import msr.atsulab.app.player.domain.model.SubtitleStyle
import kotlin.math.roundToInt

object SubtitleStyleOptions {

    val TEXT_COLOR_PRESETS = intArrayOf(
        0xFFFFFFFF.toInt(),
        0xFFFFEB3B.toInt(),
        0xFF76FF03.toInt(),
        0xFF18FFFF.toInt(),
        0xFFFF5252.toInt(),
        0xFF448AFF.toInt(),
        0xFFFFAB40.toInt(),
        0xFFFF4FA3.toInt(),
        0xFFAB47BC.toInt(),
        0xFF000000.toInt(),
        0xFF9E9E9E.toInt(),
        0xFF0EA5E9.toInt()
    )

    val BACKGROUND_COLOR_PRESETS = intArrayOf(
        0xFF000000.toInt(),
        0xFF1A1A1A.toInt(),
        0xFF0D2140.toInt(),
        0xFF3D0C0C.toInt(),
        0xFF0C3D1A.toInt(),
        0xFF2E0C3D.toInt(),
        0xFF37474F.toInt(),
        0xFF0EA5E9.toInt()
    )

    fun normalize(style: SubtitleStyle): SubtitleStyle {
        return style.copy(
            fontSize = style.fontSize.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
            fontColor = opaque(style.fontColor),
            fontStyle = style.fontStyle.coerceIn(
                SubtitleStyle.FONT_STYLE_NORMAL,
                SubtitleStyle.FONT_STYLE_BOLD_ITALIC
            ),
            backgroundColor = opaque(style.backgroundColor),
            backgroundOpacity = style.backgroundOpacity.coerceIn(MIN_PERCENT, MAX_PERCENT),
            bottomPadding = style.bottomPadding.coerceIn(MIN_PERCENT, MAX_PERCENT),
            shadow = style.shadow.coerceIn(MIN_PERCENT, MAX_PERCENT)
        )
    }

    fun backgroundArgb(style: SubtitleStyle): Int {
        if (style.hasNoBackground) return 0x00000000
        val alpha = clampPercent(style.backgroundOpacity) * 255 / 100
        return ColorChannels.argb(alpha, style.backgroundColor)
    }

    fun edgeArgb(shadowPercent: Int): Int {
        val alpha = (60 + clampPercent(shadowPercent) * 195 / 100).coerceAtMost(MAX_ALPHA)
        return ColorChannels.argb(alpha, 0xFF000000.toInt())
    }

    private fun clampPercent(value: Int) = value.coerceIn(MIN_PERCENT, MAX_PERCENT)

    private fun opaque(color: Int) = color or 0xFF000000.toInt()

    private object ColorChannels {
        fun argb(alpha: Int, color: Int): Int {
            return (alpha shl ALPHA_SHIFT) or (color and RGB_MASK)
        }
    }

    const val MIN_FONT_SIZE = 0.5f
    const val MAX_FONT_SIZE = 3.0f
    const val MIN_PERCENT = 0
    const val MAX_PERCENT = 100
    const val MIN_FONT_SIZE_PERCENT = 50
    const val MAX_FONT_SIZE_PERCENT = 300
    const val MAX_ALPHA = 255
    const val BASE_FRACTIONAL_TEXT_SIZE = 0.0533f
    private const val ALPHA_SHIFT = 24
    private const val RGB_MASK = 0x00FFFFFF

    fun percentToFontSize(percent: Int): Float {
        return (percent.coerceIn(MIN_FONT_SIZE_PERCENT, MAX_FONT_SIZE_PERCENT) / 100f)
    }

    fun fontSizeToPercent(size: Float): Int {
        return (size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE) * 100f).roundToInt()
    }
}
