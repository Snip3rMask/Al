package msr.atsulab.app.player.domain

import msr.atsulab.app.player.domain.model.SubtitleStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubtitleStyleOptionsTest {

    @Test
    fun `normalizes invalid persisted style values`() {
        val normalized = SubtitleStyleOptions.normalize(
            SubtitleStyle(
                fontSize = 9f,
                fontColor = 0x00FFFFFF,
                fontStyle = 12,
                backgroundColor = 0x00123456,
                backgroundOpacity = 240,
                hasNoBackground = true,
                bottomPadding = -5,
                shadow = 480
            )
        )

        assertEquals(3f, normalized.fontSize)
        assertEquals(0xFFFFFFFF.toInt(), normalized.fontColor)
        assertEquals(SubtitleStyle.FONT_STYLE_BOLD_ITALIC, normalized.fontStyle)
        assertEquals(0xFF123456.toInt(), normalized.backgroundColor)
        assertEquals(100, normalized.backgroundOpacity)
        assertEquals(true, normalized.hasNoBackground)
        assertEquals(0, normalized.bottomPadding)
        assertEquals(100, normalized.shadow)
    }

    @Test
    fun `background uses opacity and honors no background`() {
        val visibleBackground = SubtitleStyleOptions.backgroundArgb(
            SubtitleStyle(
                backgroundColor = 0xFF123456.toInt(),
                backgroundOpacity = 50
            )
        )
        val hiddenBackground = SubtitleStyleOptions.backgroundArgb(
            SubtitleStyle(backgroundOpacity = 100, hasNoBackground = true)
        )

        assertEquals(0x7F123456.toInt(), visibleBackground)
        assertEquals(0, hiddenBackground)
    }

    @Test
    fun `edge alpha follows anifux shadow formula`() {
        assertEquals(0x3C000000.toInt(), SubtitleStyleOptions.edgeArgb(0))
        assertEquals(0xFF000000.toInt(), SubtitleStyleOptions.edgeArgb(100))
    }

    @Test
    fun `converts size between percent and multiplier`() {
        assertEquals(0.5f, SubtitleStyleOptions.percentToFontSize(50))
        assertEquals(3f, SubtitleStyleOptions.percentToFontSize(300))
        assertEquals(125, SubtitleStyleOptions.fontSizeToPercent(1.25f))
    }
}
