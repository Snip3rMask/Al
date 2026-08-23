package msr.atsulab.app.player.data.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DakiResponseParserTest {

    @Test
    fun `parses and normalizes anime search results`() {
        val html = """
            <html>
              <body>
                <a href="/anime/example-123" title="Daki Anime">
                  <img src="//cdn.anidb.app/poster.jpg">
                </a>
                <a href="https://anidb.app/anime/full-456" title="Full Url Anime"></a>
                <a href="/anime/missing-id"></a>
              </body>
            </html>
        """.trimIndent()

        val results = DakiResponseParser.parseSearchResults(html)

        assertEquals(2, results.size)
        assertEquals("123", results[0].id)
        assertEquals("Daki Anime", results[0].title)
        assertEquals("https://cdn.anidb.app/poster.jpg", results[0].thumbnailUrl)
        assertEquals("https://anidb.app/anime/example-123", results[0].url)
        assertEquals("456", results[1].id)
    }

    @Test
    fun `ranks exact titles above partial matches`() {
        val results = listOf(
            DakiResponseParser.SearchResult("1", "Another Show", "", ""),
            DakiResponseParser.SearchResult("2", "AtsuLab Anime", "", ""),
            DakiResponseParser.SearchResult("3", "AtsuLab Anime Season 2", "", "")
        )

        val ranked = DakiResponseParser.rankSearchResults(results, "AtsuLab Anime")

        assertEquals("2", ranked.first().id)
    }

    @Test
    fun `resolves anidb id using documented key order`() {
        val json = """{"id":"ignored","anidbId":"123","anidb_id":"789"}"""

        assertEquals("123", DakiResponseParser.resolveAnidbId(json))
    }

    @Test
    fun `parses episodes with candidate metadata and fallback numbers`() {
        val json = """
            {"episodes":[
              {"id":"episode-one","number":"1.5","thumbnail":"https://example.com/1.jpg"},
              {"id":"episode-two"}
            ]}
        """.trimIndent()

        val episodes = DakiResponseParser.parseEpisodes(candidateId = "123", json = json)

        assertEquals(2, episodes.size)
        assertEquals("Episode 1", episodes[0].name)
        assertEquals(1.5f, episodes[0].number)
        assertEquals("episode-one", episodes[0].url)
        assertEquals("123", episodes[0].playbackId)
        assertEquals(DakiSourceProvider.PROVIDER_ID, episodes[0].confirmedSourceSlug)
        assertEquals("Episode 2", episodes[1].name)
        assertEquals(2f, episodes[1].number)
    }

    @Test
    fun `selects exact episode match first`() {
        val episodes = listOf(
            createEpisode(id = "special-zero", number = 0f),
            createEpisode(id = "episode-one", number = 1f),
            createEpisode(id = "episode-two", number = 2f)
        )

        assertEquals("episode-two", DakiResponseParser.selectEpisode(episodes, requestedNumber = 2).url)
        assertEquals("episode-one", DakiResponseParser.selectEpisode(episodes, requestedNumber = 1).url)
    }

    @Test
    fun `throws when requested episode cannot be selected`() {
        val episodes = listOf(createEpisode(id = "episode-one", number = 1f))

        assertThrows(IllegalArgumentException::class.java) {
            DakiResponseParser.selectEpisode(episodes, requestedNumber = 5)
        }
    }

    @Test
    fun `extracts daki sources and classifies languages`() {
        val json = """
            {"languages":[
              {"name":"Japanese Sub","embed_url":"https://embed.example/sub"},
              {"name":"English Dub","embed_url":""},
              {"code":"Eng Dub","embed_url":"https://embed.example/dub"}
            ]}
        """.trimIndent()

        val sources = DakiResponseParser.parseSources(json) { embedUrl ->
            if (embedUrl == "https://embed.example/sub") {
                "<script>https://hls.anidb.app/stream/video/master.m3u8</script>"
            } else ""
        }

        assertEquals(1, sources.size)
        with(sources.first()) {
            assertEquals("Japanese Sub", quality)
            assertEquals("Sub", language)
            assertEquals("Daki • Japanese Sub", server)
            assertEquals("Daki", displayName)
            assertEquals("https://hls.anidb.app/stream/video/master.m3u8", url)
            assertEquals("https://anidb.app/", referer)
        }
    }

    @Test
    fun `throws when languages or extracted sources are empty`() {
        assertThrows(IllegalStateException::class.java) {
            DakiResponseParser.parseSources("{}") { "" }
        }
        assertThrows(IllegalStateException::class.java) {
            DakiResponseParser.parseSources("""{"languages":[]}""") { "" }
        }
    }

    private fun createEpisode(id: String, number: Float) =
        msr.atsulab.app.player.domain.model.PlaybackEpisode(
            name = "Episode $number",
            url = id,
            number = number
        )
}
