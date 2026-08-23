package msr.atsulab.app.player.engine

import androidx.media3.common.MimeTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubtitleMimeTypesTest {

    @Test
    fun `detects srt and vtt files ignoring queries`() {
        assertEquals(MimeTypes.APPLICATION_SUBRIP, SubtitleMimeTypes.fromUrl("https://test/subtitle.srt?token=1"))
        assertEquals(MimeTypes.TEXT_VTT, SubtitleMimeTypes.fromUrl("https://test/subtitle.vtt?token=1"))
    }

    @Test
    fun `defaults unknown subtitle files to vtt`() {
        assertEquals(MimeTypes.TEXT_VTT, SubtitleMimeTypes.fromUrl("https://test/subtitles"))
    }
}
