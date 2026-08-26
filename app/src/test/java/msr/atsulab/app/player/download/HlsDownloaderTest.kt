package msr.atsulab.app.player.download

import java.io.File
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HlsDownloaderTest {

    @TempDir
    lateinit var outputDirectory: File

    @Test
    fun `downloads highest quality variant merges initialization and segments atomically`() {
        val requests = mutableListOf<Pair<String, String?>>()
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(fakeHlsInterceptor(requests))
            .build()

        var reportedTotal = 0
        val outputFile = HlsDownloader(httpClient, { outputDirectory }).download(
            request = DownloadRequest(
                aniListId = 21,
                episodeId = "1",
                displayName = "AtsuLab Anime",
                url = "https://cdn.example.com/master.m3u8",
                referer = "https://example.com/"
            ),
            onProgress = { _, total -> reportedTotal = total }
        )

        assertEquals("AtsuLab Anime - 1.mp4", outputFile.name)
        assertArrayEquals("INITONETWO".toByteArray(), outputFile.readBytes())
        assertEquals(2, reportedTotal)
        assertEquals("https://cdn.example.com/master.m3u8", requests[0].first)
        assertEquals("https://cdn.example.com/720/media.m3u8", requests[1].first)
        assertEquals(
            setOf(
                "https://cdn.example.com/720/init.bin",
                "https://cdn.example.com/720/segment-1.bin",
                "https://cdn.example.com/720/segment-2.bin"
            ),
            requests.drop(2).map { it.first }.toSet()
        )
        assertTrue(requests.drop(2).all { it.second == "https://example.com/" })
        assertTrue(outputFile.exists())
    }

    private fun fakeHlsInterceptor(requests: MutableList<Pair<String, String?>>) = Interceptor { chain ->
        val url = chain.request().url.toString()
        requests += url to chain.request().header("Referer")
        val content = when (url) {
            "https://cdn.example.com/master.m3u8" -> """
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=500000,RESOLUTION=640x360
                https://cdn.example.com/360/media.m3u8
                #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
                https://cdn.example.com/720/media.m3u8
            """.trimIndent()
            "https://cdn.example.com/720/media.m3u8" -> """
                #EXTM3U
                #EXT-X-MAP:URI="https://cdn.example.com/720/init.bin"
                https://cdn.example.com/720/segment-1.bin
                https://cdn.example.com/720/segment-2.bin
            """.trimIndent()
            "https://cdn.example.com/720/init.bin" -> "INIT"
            "https://cdn.example.com/720/segment-1.bin" -> "ONE"
            "https://cdn.example.com/720/segment-2.bin" -> "TWO"
            else -> throw IllegalStateException("Unexpected URL: $url")
        }
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(content.toResponseBody("text/plain".toMediaType()))
            .build()
    }


}
