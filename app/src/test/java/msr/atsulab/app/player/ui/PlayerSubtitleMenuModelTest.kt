package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.SubtitleTrack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerSubtitleMenuModelTest {

    @Test
    fun `builds track options and places off last`() {
        val options = PlayerSubtitleMenuModel.options(
            tracks = listOf(
                SubtitleTrack(id = "1:0", label = "English", language = "en", isSelected = false),
                SubtitleTrack(id = "2:1", label = "Japanese", language = "ja", isSelected = true)
            ),
            hasExternalSubtitle = false
        )

        assertEquals(listOf("English", "Japanese", "Off"), options.map { it.label })
        assertEquals(listOf(false, true, false), options.map { it.isSelected })
        assertEquals(listOf("1:0", "2:1", null), options.map { it.id })
    }

    @Test
    fun `marks off selected when no embedded track is active`() {
        val options = PlayerSubtitleMenuModel.options(
            tracks = listOf(
                SubtitleTrack(id = "0:0", label = "English", language = "en", isSelected = false)
            ),
            hasExternalSubtitle = false
        )

        assertEquals(2, options.size)
        assertEquals(false, options.first().isSelected)
        assertEquals(true, options.last().isSelected)
    }

    @Test
    fun `adds english fallback for external subtitle without detected tracks`() {
        val options = PlayerSubtitleMenuModel.options(
            tracks = emptyList(),
            hasExternalSubtitle = true
        )

        assertEquals(listOf("English", "Off"), options.map { it.label })
        assertEquals(msr.atsulab.app.player.engine.EXTERNAL_SUBTITLE_TRACK_ID, options.first().id)
        assertEquals(null, options.last().id)
    }

    @Test
    fun `does not add external fallback when tracks are detected`() {
        val options = PlayerSubtitleMenuModel.options(
            tracks = listOf(
                SubtitleTrack(id = "0:0", label = "English", language = "en", isSelected = true)
            ),
            hasExternalSubtitle = true
        )

        assertEquals(listOf("English", "Off"), options.map { it.label })
        assertEquals(listOf("0:0", null), options.map { it.id })
    }
}
