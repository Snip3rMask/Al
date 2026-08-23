package msr.atsulab.app.player.data.provider

import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DakiSourceProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: DakiSourceProvider

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        provider = DakiSourceProvider(
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
    fun `finds direct anidb candidate from anilist id`() {
        server.enqueue(
            MockResponse().setBody("""{"anidbId":"123"}""").setHeader("Content-Type", "application/json")
        )

        val candidates = provider.findCandidates(title = "AtsuLab Anime", aniListId = 21)
            .test()
            .await()

        candidates.assertComplete()
        candidates.assertValue(listOf(SourceCandidate(id = "123", title = "AtsuLab Anime")))

        val request = server.takeRequest()
        assertEquals("/api/frontend/resolve?anilistId=21", request.path ?: "")
        assertEquals(providerBaseUrl + "/", request.getHeader("Referer") ?: "")
        assertEquals("text/html,application/json,*/*", request.getHeader("Accept") ?: "")
    }

    @Test
    fun `falls back to ranked search when direct resolution fails`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse().setBody(
                """
                <a href="/anime/wrong-1" title="Wrong Show"></a>
                <a href="/anime/right-123" title="AtsuLab Anime"></a>
                """.trimIndent()
            ).setHeader("Content-Type", "text/html")
        )

        val candidates = provider.findCandidates(title = "AtsuLab Anime", aniListId = 21)
            .test()
            .await()

        candidates.assertComplete()
        assertEquals("123", candidates.values()[0].first().id)
        assertEquals("/api/frontend/resolve?anilistId=21", server.takeRequest().path ?: "")
        assertEquals("/browse?q=AtsuLab+Anime", server.takeRequest().path ?: "")
    }

    @Test
    fun `preserves anilist id without searching when title is blank`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val candidates = provider.findCandidates(title = "", aniListId = 21)
            .test()
            .await()

        candidates.assertComplete()
        candidates.assertValue(listOf(SourceCandidate(id = "21", title = "")))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `loads episodes for a daki candidate`() {
        server.enqueue(
            MockResponse().setBody(
                """{"episodes":[{"id":"episode-42","number":2}]}"""
            )
        )

        val episodes = provider.getEpisodes(SourceCandidate(id = "123", title = "AtsuLab Anime"))
            .test()
            .await()

        episodes.assertComplete()
        assertEquals("episode-42", episodes.values()[0].single().url)
        assertEquals("/api/frontend/anime/123/episodes", server.takeRequest().path ?: "")
    }

    @Test
    fun `defers invalid candidate failures until subscription`() {
        val single = provider.getEpisodes(SourceCandidate(id = "", title = "AtsuLab Anime"))

        assertDoesNotThrow { single }

        single.test().await().assertError(IllegalArgumentException::class.java)
    }

    @Test
    fun `resolves selected episode to hls sources`() {
        server.enqueue(
            MockResponse().setBody(
                """{"episodes":[
                    {"id":"episode-41","number":1},
                    {"id":"episode-42","number":2}
                ]}
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """{"languages":[{"name":"Japanese","embed_url":"$providerBaseUrl/embed"}]}"""
            )
        )
        server.enqueue(
            MockResponse().setBody("https://hls.anidb.app/stream/video-42/master.m3u8")
        )

        val sources = provider.getSources(
            candidate = SourceCandidate(id = "123", title = "AtsuLab Anime"),
            episode = PlaybackEpisode(name = "Episode 2", url = "", number = 2f)
        ).test().await()

        sources.assertComplete()
        assertEquals("https://hls.anidb.app/stream/video-42/master.m3u8", sources.values()[0].single().url)
        assertEquals("/api/frontend/anime/123/episodes", server.takeRequest().path ?: "")
        assertEquals("/api/frontend/episode/episode-42/languages", server.takeRequest().path ?: "")
        assertEquals("/embed", server.takeRequest().path ?: "")
    }

    @Test
    fun `detects cloudflare challenge as an error`() {
        server.enqueue(
            MockResponse().setBody("Just a moment... var _cf_chl_opt = {};")
        )

        val candidates = provider.findCandidates(title = "AtsuLab Anime", aniListId = null)
            .test()
            .await()

        candidates.assertError(IOException::class.java)
    }

    private val providerBaseUrl: String
        get() = server.url("/").toString().removeSuffix("/")
}
