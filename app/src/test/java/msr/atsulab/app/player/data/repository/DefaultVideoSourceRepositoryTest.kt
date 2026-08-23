package msr.atsulab.app.player.data.repository

import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.VideoSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultVideoSourceRepositoryTest {

    private val anime = PlaybackAnime(aniListId = 21, title = "AtsuLab Anime")
    private val episode = PlaybackEpisode(name = "Episode 1", url = "ep-1", number = 1f, providerId = "anifux")

    @Test
    fun `routes tagged episode to matching provider first`() {
        val mkissa = FakeSourceProvider(id = "mkissa")
        val anifux = FakeSourceProvider(
            id = "anifux",
            sources = listOf(VideoSource(quality = "1080p", url = "https://cdn.example/anifux.m3u8"))
        )
        val repository = DefaultVideoSourceRepository(listOf(mkissa, anifux))

        val sources = repository.getSources(anime, episode).test().await().values().single()

        assertEquals(0, mkissa.sourceCalls)
        assertEquals(1, anifux.sourceCalls)
        assertEquals("https://cdn.example/anifux.m3u8", sources.single().url)
        assertEquals("anifux", sources.single().providerId)
        assertEquals(episode, anifux.requestedEpisodes.single())
    }

    @Test
    fun `falls back when tagged provider fails or returns empty`() {
        val failed = FakeSourceProvider(id = "anifux", sourceError = true)
        val empty = FakeSourceProvider(id = "mkissa")
        val daki = FakeSourceProvider(
            id = "daki",
            sources = listOf(VideoSource(quality = "HLS", url = "https://cdn.example/daki.m3u8"))
        )
        val repository = DefaultVideoSourceRepository(listOf(failed, empty, daki))

        val sources = repository.getSources(anime, episode).test().await().values().single()

        assertEquals(1, failed.sourceCalls)
        assertEquals(1, empty.sourceCalls)
        assertEquals(1, daki.sourceCalls)
        assertEquals("daki", sources.single().providerId)
    }

    @Test
    fun `prioritizes sub preference without removing dub options`() {
        val dub = VideoSource(quality = "Dub 1080p", url = "dub", language = "English Dub")
        val sub = VideoSource(quality = "1080p", url = "sub", language = "Japanese Sub")
        val provider = FakeSourceProvider(id = "anifux", sources = listOf(dub, sub))
        val repository = DefaultVideoSourceRepository(listOf(provider))

        val sources = repository.getSources(anime, episode, preferredLanguage = "sub")
            .test().await().values().single()

        assertEquals(listOf("sub", "dub"), sources.map { it.url })
    }

    @Test
    fun `server preference has higher priority than language`() {
        val serverSub = VideoSource(quality = "Sub", url = "server-sub", language = "Sub", displayName = "Nora")
        val otherDub = VideoSource(quality = "Dub", url = "other-dub", language = "Dub", displayName = "Zuki")
        val provider = FakeSourceProvider(id = "anifux", sources = listOf(otherDub, serverSub))
        val repository = DefaultVideoSourceRepository(listOf(provider))

        val sources = repository.getSources(
            anime,
            episode,
            preferredLanguage = "dub",
            preferredServer = "Nora"
        ).test().await().values().single()

        assertEquals(listOf("server-sub", "other-dub"), sources.map { it.url })
    }
}
