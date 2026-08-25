package msr.atsulab.app.player.domain.model

data class SubtitleStyle(
    val fontSize: Float = DEFAULT_FONT_SIZE,
    val fontColor: Int = DEFAULT_FONT_COLOR,
    val fontStyle: Int = FONT_STYLE_NORMAL,
    val backgroundColor: Int = DEFAULT_BACKGROUND_COLOR,
    val backgroundOpacity: Int = DEFAULT_BACKGROUND_OPACITY,
    val hasNoBackground: Boolean = false,
    val bottomPadding: Int = DEFAULT_BOTTOM_PADDING,
    val shadow: Int = DEFAULT_SHADOW
) {
    companion object {
        const val FONT_STYLE_NORMAL = 0
        const val FONT_STYLE_BOLD = 1
        const val FONT_STYLE_ITALIC = 2
        const val FONT_STYLE_BOLD_ITALIC = 3
        const val DEFAULT_FONT_SIZE = 1.0f
        const val DEFAULT_FONT_COLOR = 0xFFFFFFFF.toInt()
        const val DEFAULT_BACKGROUND_COLOR = 0xFF000000.toInt()
        const val DEFAULT_BACKGROUND_OPACITY = 0
        const val DEFAULT_BOTTOM_PADDING = 0
        const val DEFAULT_SHADOW = 0
    }
}
