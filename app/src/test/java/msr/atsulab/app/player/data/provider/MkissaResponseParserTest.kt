package msr.atsulab.app.player.data.provider

import msr.atsulab.app.player.domain.model.SourceCandidate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MkissaResponseParserTest {

    @Test
    fun `parses sub episodes before dub and preserves raw episode values`() {
        val json = """
            {"data":{"show":{
              "_id":"show-42",
              "availableEpisodesDetail":{"sub":["10","02","1.5"],"dub":["99"]}
            }}}
        """.trimIndent()

        val episodes = MkissaResponseParser.parseEpisodes(request("21"), json)

        assertEquals(listOf("Episode 1.5", "Episode 02", "Episode 10"), episodes.map { it.name })
        assertEquals(listOf("show-42/ep-1.5", "show-42/ep-02", "show-42/ep-10"), episodes.map { it.url })
        assertEquals(listOf(1.5f, 2f, 10f), episodes.map { it.number })
        episodes.forEach { episode ->
            assertEquals("show-42", episode.playbackId)
            assertEquals(MkissaSourceProvider.PROVIDER_ID, episode.confirmedSourceSlug)
            assertEquals(21, episode.aniListId)
        }
    }

    @Test
    fun `uses dub when sub is missing or empty`() {
        val missingSub = """{"data":{"show":{"_id":"s1","availableEpisodesDetail":{"dub":["2"]}}}}"""
        val emptySub = """{"data":{"show":{"_id":"s2","availableEpisodesDetail":{"sub":[],"dub":["3"]}}}}"""

        val first = MkissaResponseParser.parseEpisodes(request("21"), missingSub)
        val second = MkissaResponseParser.parseEpisodes(request("21"), emptySub)

        assertEquals(listOf("s1/ep-2"), first.map { it.url })
        assertEquals(listOf("s2/ep-3"), second.map { it.url })
    }

    @Test
    fun `falls back to requested anilist id and skips invalid values`() {
        val json = """
            {"data":{"show":{
              "availableEpisodesDetail":{"sub":["bad","","4"]}
            }}}
        """.trimIndent()

        val episodes = MkissaResponseParser.parseEpisodes(request("77"), json)

        assertEquals(1, episodes.size)
        assertEquals("77/ep-4", episodes.single().url)
        assertEquals("77", episodes.single().playbackId)
    }

    @Test
    fun `returns no episodes without available detail`() {
        assertEquals(0, MkissaResponseParser.parseEpisodes(request("21"), """{"data":{}}""").size)
        assertEquals(0, MkissaResponseParser.parseEpisodes(request("21"), """{"data":{"show":{}}}""").size)
        assertEquals(0, MkissaResponseParser.parseEpisodes(request("21"), """{"data":{"show":{"availableEpisodesDetail":{}}}}""").size)
    }

    private fun request(id: String) = SourceCandidate(id = id, title = "AtsuLab Anime")
}
