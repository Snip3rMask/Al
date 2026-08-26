package msr.atsulab.app.player.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HlsPlaylistParserTest {

    @Test
    fun `parses master variants with resolution labels and absolute urls`() {
        val baseUrl = "https://cdn.example.com/video/master.m3u8"
        val content = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=2200000,RESOLUTION=1280x720
            720/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=4500001,RESOLUTION=1920x1080
            https://mirror.example.com/1080/index.m3u8
        """.trimIndent()

        val variants = HlsPlaylistParser.parseMaster(content, baseUrl)

        assertEquals(2, variants.size)
        assertEquals("https://cdn.example.com/video/720/index.m3u8", variants[0].url)
        assertEquals("720p", variants[0].label)
        assertEquals(2_200_000, variants[0].bandwidth)
        assertEquals(1280, variants[0].width)
        assertEquals(720, variants[0].height)
        assertEquals("https://mirror.example.com/1080/index.m3u8", variants[1].url)
        assertEquals("1080p", variants[1].label)
    }

    @Test
    fun `parses media initialization and ordered segment urls`() {
        val content = """
            #EXTM3U
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:4.000,
            segment-1.ts
            #EXTINF:4.000,
            /segments/segment-2.ts
        """.trimIndent()

        val playlist = HlsPlaylistParser.parseMedia(content, "https://cdn.example.com/video/media.m3u8")

        assertEquals("https://cdn.example.com/video/init.mp4", playlist.initializationUrl)
        assertEquals(
            listOf(
                "https://cdn.example.com/video/segment-1.ts",
                "https://cdn.example.com/segments/segment-2.ts"
            ),
            playlist.segmentUrls
        )
        assertFalse(playlist.isEmpty)
    }

    @Test
    fun `rejects encrypted streams`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            HlsPlaylistParser.parseMedia(
                "#EXTM3U\n#EXT-X-KEY:METHOD=AES-128,URI=\"key\"\nsegment.ts",
                "https://example.com/media.m3u8"
            )
        }
        assertEquals("Encrypted HLS streams are not supported", exception.message)
    }

    @Test
    fun `allows explicitly unencrypted streams`() {
        val playlist = HlsPlaylistParser.parseMedia(
            "#EXTM3U\n#EXT-X-KEY:METHOD=NONE\nsegment.ts",
            "https://example.com/media.m3u8"
        )

        assertFalse(playlist.isEmpty)
    }
}
