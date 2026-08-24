package msr.atsulab.app.player.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SubtitleTrackMetadataTest {

    @Test
    fun `combines track label and language like anifux`() {
        assertEquals("English (en)", SubtitleTrackMetadata.displayLabel("English", "en"))
    }

    @Test
    fun `uses language when label is missing`() {
        assertEquals("en", SubtitleTrackMetadata.displayLabel(null, "EN"))
    }

    @Test
    fun `avoids duplicated label inside language`() {
        assertEquals("english", SubtitleTrackMetadata.displayLabel("english", "english"))
    }

    @Test
    fun `uses label without language`() {
        assertEquals("English", SubtitleTrackMetadata.displayLabel("English", ""))
    }

    @Test
    fun `rejects tracks without usable metadata`() {
        assertNull(SubtitleTrackMetadata.displayLabel("", " "))
    }
}
