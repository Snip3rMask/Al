package msr.atsulab.app.player.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DownloadModelsTest {

    @Test
    fun `download request creates safe stable file name`() {
        val request = DownloadRequest(
            aniListId = 21,
            episodeId = "Episode 1?",
            displayName = "AtsuLab: Anime *Season*",
            url = "https://example.com/stream.m3u8",
            referer = "https://example.com/"
        )

        assertEquals("AtsuLab Anime Season - Episode 1.mp4", request.fileName)
    }
}
