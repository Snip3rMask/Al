package msr.atsulab.app.player.download

import java.io.File
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HlsDownloaderTest {

    @TempDir
    lateinit var outputDirectory: File

    private lateinit var server: MockWebServer

    private lateinit var downloader: HlsDownloader

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        downloader = HlsDownloader(OkHttpClient(), outputDirectory)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `downloads highest quality variant merges initialization and segments atomically`() {
        val masterUrl = server.url("/master.m3u8").toString()
        val mediaUrl = "/media.m3u8"
        server.enqueue(
            MockResponse().setBody(
                """
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=500000,RESOLUTION=640x360
                $mediaUrl
                #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
                $mediaUrl
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """
                #EXTM3U
                #EXT-X-MAP:URI="init.bin"
                segment-1.bin
                segment-2.bin
                """.trimIndent()
            )
        )
        val initialization = buffer("INIT")
        val firstSegment = buffer("ONE")
        val secondSegment = buffer("TWO")
        server.enqueue(MockResponse().setBody(initialization))
        server.enqueue(MockResponse().setBody(firstSegment))
        server.enqueue(MockResponse().setBody(secondSegment))

        var reportedTotal = 0
        val outputFile = downloader.download(
            request = DownloadRequest(
                aniListId = 21,
                episodeId = "1",
                displayName = "AtsuLab Anime",
                url = masterUrl,
                referer = "https://example.com/"
            ),
            onProgress = { _, total -> reportedTotal = total }
        )

        assertEquals("AtsuLab Anime - 1.mp4", outputFile.name)
        assertArrayEquals("INITONETWO".toByteArray(), outputFile.readBytes())
        assertEquals(2, reportedTotal)
        assertEquals("/media.m3u8", server.takeRequest().path)
        assertEquals("https://example.com/", server.takeRequest().getHeader("Referer"))
        assertEquals(File(outputDirectory.parentFile, outputDirectory.name).resolve(outputFile.name), outputFile)
    }

    private fun buffer(value: String): Buffer = Buffer().writeUtf8(value)
}
