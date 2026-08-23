package msr.atsulab.app.player.data.provider

import msr.atsulab.app.player.domain.model.SourceCandidate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnifuxResponseParserTest {

    @Test
    fun `parses candidates using documented display order`() {
        val json = """
            {
              "anineko":[{"id":"slug","title":"Nora Anime","thumbnail":"nora.jpg"}],
              "anidb":[{"id":"123","title":"Daki Anime","thumbnail":"daki.jpg"},{"id":"","title":"invalid"}]
            }
        """.trimIndent()

        val groups = AnifuxResponseParser.parseCandidateGroups(json)

        assertEquals(listOf("Daki", "Nora"), groups.keys.toList())
        assertEquals("anidb", groups.getValue("Daki").single().backendProvider)
        assertEquals("anineko", groups.getValue("Nora").single().backendProvider)
        assertEquals(
            listOf("123", "slug"),
            AnifuxResponseParser.flattenCandidateGroups(groups).map { it.id }
        )
    }

    @Test
    fun `parses episodes with java-compatible fallback number and name`() {
        val candidate = SourceCandidate(id = "123", title = "AtsuLab Anime", backendProvider = "anidb")
        val json = """[{"id":"ep-one","number":2.5},{"id":"ep-two"}]"""

        val episodes = AnifuxResponseParser.parseEpisodes(candidate, json)

        assertEquals(2, episodes.size)
        assertEquals("Episode 2", episodes[0].name)
        assertEquals(2.5f, episodes[0].number)
        assertEquals("123", episodes[0].playbackId)
        assertEquals("Episode 2", episodes[1].name)
        assertEquals(2f, episodes[1].number)
    }

    @Test
    fun `keeps confirmed slug only for anineko candidates`() {
        val candidate = SourceCandidate(
            id = "123",
            title = "AtsuLab Anime",
            backendProvider = AnifuxResponseParser.BACKEND_ANINEKO
        )
        val json = """[{"id":"episode-one"}]"""

        val episodes = AnifuxResponseParser.parseEpisodes(
            candidate = candidate,
            json = json,
            confirmedAninekoSlug = "attack-on-titan"
        )

        assertEquals("attack-on-titan", episodes.single().confirmedSourceSlug)
    }

    @Test
    fun `parses source metadata nested subtitles captions and skips`() {
        val json = """
            [{
              "url":"https://cdn.example/stream.m3u8",
              "label":"1080p","language":"Sub","server":"Nora",
              "displayName":"Nora","referer":"https://player.example/",
              "subtitles":[{"src":"https://cdn.example/default.vtt"}],
              "skipTimes":[
                {"type":"op","start":10.5,"end":120},
                {"type":"ed","start":1300,"end":1380.25}
              ]
            }]
        """.trimIndent()

        val source = AnifuxResponseParser.parseSources(json).single()

        assertEquals("https://cdn.example/stream.m3u8", source.url)
        assertEquals("https://cdn.example/default.vtt", source.subtitleUrl)
        assertEquals(listOf(10500L to 120000L, 1300000L to 1380250L), source.skipIntervals.map { it.startMs to it.endMs })
    }

    @Test
    fun `extracts encoded caption url from source url`() {
        val json = """
            [{
              "url":"https://player.example/video?caption_1=https%3A%2F%2Fcdn.example%2Fcaption.vtt&token=abc",
              "label":"Source"
            }]
        """.trimIndent()

        val source = AnifuxResponseParser.parseSources(json).single()

        assertEquals("https://cdn.example/caption.vtt", source.subtitleUrl)
    }

    @Test
    fun `returns empty collections for malformed payloads`() {
        assertEquals(emptyMap<String, List<SourceCandidate>>(), AnifuxResponseParser.parseCandidateGroups("{"))
        assertEquals(emptyList<msr.atsulab.app.player.domain.model.PlaybackEpisode>(), AnifuxResponseParser.parseEpisodes(SourceCandidate("1", "One"), "{"))
        assertEquals(emptyList<msr.atsulab.app.player.domain.model.VideoSource>(), AnifuxResponseParser.parseSources("{"))
    }
}
