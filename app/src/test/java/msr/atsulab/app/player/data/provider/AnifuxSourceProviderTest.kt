package msr.atsulab.app.player.data.provider

import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnifuxSourceProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: AnifuxSourceProvider

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        provider = AnifuxSourceProvider(
            client = OkHttpClient(),
            baseUrl = server.url("/").toString().removeSuffix("/"),
            ioScheduler = Schedulers.trampoline()
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `finds grouped candidates with browser compatible json headers`() {
        server.enqueue(
            MockResponse().setBody(
                """{"anidb":[{"id":"123","title":"Daki"}],"anineko":[{"id":"slug","title":"Nora"}]}"""
            )
        )

        val groups = provider.findGroupedCandidates(title = "AtsuLab Anime", aniListId = 21)
            .test().await().values().single()

        assertEquals(listOf("Daki", "Nora"), groups.keys.toList())
        val request = server.takeRequest()
        assertEquals("/api/anime/source-candidates?title=AtsuLab+Anime&anilistId=21", request.path)
        assertEquals("Anifux/1.0", request.getHeader("User-Agent"))
        assertEquals("application/json", request.getHeader("Accept"))
    }

    @Test
    fun `loads anidb episodes and preserves candidate metadata`() {
        server.enqueue(MockResponse().setBody("""[{"id":"episode-two","number":2.5}]"""))
        val candidate = SourceCandidate(id = "123", title = "AtsuLab Anime", backendProvider = "anidb")

        val episodes = provider.getEpisodes(candidate).test().await().values().single()

        assertEquals(1, episodes.size)
        assertEquals("Episode 2", episodes.single().name)
        assertEquals("/api/anime/123/episodes", server.takeRequest().path)
    }

    @Test
    fun `skips episode lookup for non anidb backend candidate`() {
        val candidate = SourceCandidate(id = "slug", title = "AtsuLab Anime", backendProvider = "anineko")

        val episodes = provider.getEpisodes(candidate).test().await().values().single()

        assertTrue(episodes.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `resolves sources through primary backend with confirmed anineko slug`() {
        server.enqueue(
            MockResponse().setBody(
                """[{"url":"https://cdn.example/video.m3u8","label":"1080p","language":"Sub","server":"Nora"}]"""
            )
        )
        val episode = PlaybackEpisode(
            name = "Episode 2",
            url = "123/ep-2",
            number = 2f,
            postTitle = "AtsuLab Anime",
            confirmedSourceSlug = "nora-slug"
        )

        val sources = provider.getSources(
            candidate = SourceCandidate(id = "123", title = "AtsuLab Anime"),
            episode = episode
        ).test().await().values().single()

        assertEquals(1, sources.size)
        assertEquals("https://cdn.example/video.m3u8", sources.single().url)
        assertEquals(
            "/api/anime/episode/123%2Fep-2/sources?title=AtsuLab+Anime&ep=2&aninekoSlug=nora-slug",
            server.takeRequest().path
        )
    }

    @Test
    fun `propagates missing source payload for repository fallback`() {
        server.enqueue(MockResponse().setBody("[]"))

        provider.getSources(
            candidate = SourceCandidate(id = "123", title = "AtsuLab Anime"),
            episode = PlaybackEpisode(name = "Episode 1", url = "ep-1", number = 1f, postTitle = "AtsuLab")
        ).test().await().assertError(IOException::class.java)
    }

    @Test
    fun `falls back from aniList resolution to title search`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setBody("""[{"id":"db9","title":"AtsuLab Anime"}]"""))

        val resolvedId = provider.resolveAnidbId(aniListId = "21", title = "AtsuLab Anime")
            .test().await().values().single()

        assertEquals("db9", resolvedId)
        assertEquals("/api/anime/resolve/aniList/21", server.takeRequest().path)
        assertEquals("/api/anime/search?q=AtsuLab+Anime", server.takeRequest().path)
    }

    @Test
    fun `defers blank candidate failure until subscription`() {
        val single = provider.getEpisodes(SourceCandidate(id = "", title = "AtsuLab Anime"))

        assertDoesNotThrow { single }
        single.test().await().assertError(IllegalArgumentException::class.java)
    }
}
