package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.VideoSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerServerMenuModelTest {

    @Test
    fun `detects dub from source metadata`() {
        assertTrue(PlayerServerMenuModel.isDub(source(language = "English Dub")))
        assertFalse(PlayerServerMenuModel.isDub(source(language = "Japanese Sub")))
    }

    @Test
    fun `filters servers by language mode and numbers repeated names`() {
        val sources = listOf(
            source(displayName = "Zoro", language = "Sub"),
            source(displayName = "Mega", language = "Dub"),
            source(displayName = "Zoro", language = "Japanese")
        )

        val subOptions = PlayerServerMenuModel.options(sources, selectedIndex = 2, showDub = false)
        val dubOptions = PlayerServerMenuModel.options(sources, selectedIndex = 1, showDub = true)

        assertEquals(listOf("Zoro 1", "Zoro 2"), subOptions.map { it.label })
        assertEquals(listOf(0, 2), subOptions.map { it.sourceIndex })
        assertEquals(listOf(false, false), subOptions.map { it.isSelected })
        assertEquals(listOf("Mega 1"), dubOptions.map { it.label })
        assertEquals(listOf(true), dubOptions.map { it.isSelected })
    }

    @Test
    fun `control label prefers display name then server`() {
        assertEquals("Zoro", PlayerServerMenuModel.controlLabel(listOf(source(displayName = "Zoro")), 0))
        assertEquals(
            "Fallback Server",
            PlayerServerMenuModel.controlLabel(
                listOf(source(server = "Fallback Server")),
                0
            )
        )
        assertEquals("Source", PlayerServerMenuModel.controlLabel(emptyList(), 0))
    }

    private fun source(
        displayName: String = "",
        language: String = "Sub",
        server: String = ""
    ): VideoSource = VideoSource(
        quality = "1080p",
        url = "https://example.test/video.m3u8",
        language = language,
        server = server,
        displayName = displayName
    )
}
