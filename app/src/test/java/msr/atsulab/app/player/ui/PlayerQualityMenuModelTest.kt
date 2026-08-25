package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.VideoQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerQualityMenuModelTest {

    private val qualities = listOf(
        VideoQuality(id = "0:1", label = "1080p", width = 1920, height = 1080, bitrate = 1L, isSelected = false),
        VideoQuality(id = "1:0", label = "720p", width = 1280, height = 720, bitrate = 1L, isSelected = true)
    )

    @Test
    fun `options put auto first and mark manual selection`() {
        val options = PlayerQualityMenuModel.options(
            qualities = qualities,
            selectedTrackId = "0:1"
        )

        assertEquals("AUTO", options.first().label)
        assertEquals(null, options.first().id)
        assertEquals(false, options.first().isSelected)
        assertEquals(listOf("1080p", "720p"), options.drop(1).map { it.label })
        assertEquals(true, options.first { it.id == "0:1" }.isSelected)
    }

    @Test
    fun `auto is selected when no manual track is selected`() {
        val options = PlayerQualityMenuModel.options(
            qualities = qualities,
            selectedTrackId = null
        )

        assertEquals(true, options.first().isSelected)
        assertEquals(false, options.any { it.id != null && it.isSelected })
    }

    @Test
    fun `control labels match compact quality names`() {
        assertEquals("AUTO", PlayerQualityMenuModel.controlLabel(qualities, null))
        assertEquals("FHD", PlayerQualityMenuModel.controlLabel(qualities, "0:1"))
        assertEquals("HD", PlayerQualityMenuModel.controlLabel(qualities, "1:0"))
        assertEquals("AUTO", PlayerQualityMenuModel.controlLabel(emptyList(), "missing"))
    }
}
