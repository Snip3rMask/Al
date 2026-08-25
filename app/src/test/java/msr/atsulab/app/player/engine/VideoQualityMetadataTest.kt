package msr.atsulab.app.player.engine

import msr.atsulab.app.player.domain.model.VideoQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VideoQualityMetadataTest {

    @Test
    fun `creates resolution labels like anifux hls parser`() {
        assertEquals("1080p", VideoQualityMetadata.displayLabel(1080, 0L, ""))
        assertEquals("720p", VideoQualityMetadata.displayLabel(-1, 2_200_000L, ""))
        assertEquals("480p", VideoQualityMetadata.displayLabel(-1, 1_000_000L, ""))
        assertEquals("360p", VideoQualityMetadata.displayLabel(-1, 500_000L, ""))
    }

    @Test
    fun `uses explicit fallback only without resolution or bitrate`() {
        assertEquals("High", VideoQualityMetadata.displayLabel(-1, -1L, " High "))
        assertNull(VideoQualityMetadata.displayLabel(-1, -1L, " "))
    }

    @Test
    fun `create rejects variants without usable metadata`() {
        assertNull(
            VideoQualityMetadata.create(
                id = "0:0",
                fallbackLabel = "",
                width = -1,
                height = -1,
                bitrate = -1,
                isSelected = false
            )
        )
        assertNotNull(
            VideoQualityMetadata.create(
                id = "0:0",
                fallbackLabel = "",
                width = 1920,
                height = 1080,
                bitrate = 4_000_000,
                isSelected = true
            )
        )
    }

    @Test
    fun `sorts qualities from highest to lowest`() {
        val sorted = VideoQualityMetadata.sorted(
            listOf(
                VideoQuality("low", "360p", 640, 360, 700_000, false),
                VideoQuality("full", "1080p", 1920, 1080, 4_000_000, false),
                VideoQuality("hd-low", "720p", 1280, 720, 2_100_000, false),
                VideoQuality("hd-high", "720p", 1280, 720, 2_400_000, false)
            )
        )

        assertEquals(listOf("full", "hd-high", "hd-low", "low"), sorted.map { it.id })
    }
}
