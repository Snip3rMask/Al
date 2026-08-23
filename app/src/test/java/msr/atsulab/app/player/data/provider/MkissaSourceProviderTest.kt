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

class MkissaSourceProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: MkissaSourceProvider

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        provider = MkissaSourceProvider(
            client = OkHttpClient(),
            apiUrl = server.url("/api").toString(),
            siteOrigin = server.url("/").toString().removeSuffix("/"),
            ioScheduler = Schedulers.trampoline()
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `finds direct candidate from anilist id`() {
        provider.findCandidates(title = "AtsuLab Anime", aniListId = 21)
            .test()
            .await()
            .assertValue(listOf(SourceCandidate(id = "21", title = "AtsuLab Anime")))
    }

    @Test
    fun `returns no candidate without anilist id`() {
        provider.findCandidates(title = "AtsuLab Anime", aniListId = null)
            .test()
            .await()
            .assertValue(emptyList())
    }

    @Test
    fun `loads episodes with graphql payload and browser headers`() {
        server.enqueue(MockResponse().setBody("""{"data":{"show":{"_id":"show-9","availableEpisodesDetail":{"sub":["1"]}}}}"""))

        val episodes = provider.getEpisodes(SourceCandidate(id = "21", title = "AtsuLab Anime"))
            .test()
            .await()

        episodes.assertComplete()
        assertEquals("show-9/ep-1", episodes.values()[0].single().url)

        val request = server.takeRequest()
        assertEquals("/api", request.path ?: "")
        assertEquals("POST", request.method)
        assertEquals("application/json", request.getHeader("Content-Type") ?: "")
        assertEquals(providerSiteOrigin, request.getHeader("Origin") ?: "")
        assertEquals("$providerSiteOrigin/", request.getHeader("Referer") ?: "")
        assertEquals("Mozilla/5.0", request.getHeader("User-Agent") ?: "")
        val body = request.body?.readUtf8().orEmpty()
        assertEquals(true, body.contains("\"query\":\"query ($_id: String!)"))
        assertEquals(true, body.contains("\"variables\":{\"_id\":\"21\"}"))
    }

    @Test
    fun `propagates http failures for fallback handling`() {
        server.enqueue(MockResponse().setResponseCode(500))

        provider.getEpisodes(SourceCandidate(id = "21", title = "AtsuLab Anime"))
            .test()
            .await()
            .assertError(IOException::class.java)
    }

    @Test
    fun `defers invalid candidate failure until subscription`() {
        val single = provider.getEpisodes(SourceCandidate(id = "", title = "AtsuLab Anime"))

        assertDoesNotThrow { single }
        single.test().await().assertError(IllegalArgumentException::class.java)
    }

    @Test
    fun `mkissa contributes no playback sources directly`() {
        provider.getSources(
            candidate = SourceCandidate(id = "21", title = "AtsuLab Anime"),
            episode = PlaybackEpisode(name = "Episode 1", url = "show/ep-1")
        ).test().await().assertValue(emptyList())
        assertEquals(0, server.requestCount)
    }

    private val providerSiteOrigin: String
        get() = server.url("/").toString().removeSuffix("/")
}
