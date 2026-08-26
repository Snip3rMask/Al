package msr.atsulab.app.player.download

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CompletedDownloadTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `creates persistent record from request and downloaded file`() {
        val file = File(directory, "AtsuLab Anime - 1.mp4").apply { writeText("video") }
        val request = DownloadRequest(
            aniListId = 21,
            episodeId = "1",
            displayName = "AtsuLab Anime",
            url = "https://example.com/stream.m3u8",
            quality = "720p"
        )

        val entry = CompletedDownload.from(request, file, completedAtEpochMs = 123L)

        assertEquals(file.absolutePath, entry.key)
        assertEquals(5L, entry.sizeBytes)
        assertEquals("720p", entry.quality)
        assertEquals(123L, entry.completedAtEpochMs)
        assertTrue(entry.file?.exists() == true)
    }

}
