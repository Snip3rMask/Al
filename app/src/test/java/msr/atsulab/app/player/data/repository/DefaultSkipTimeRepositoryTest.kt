package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.schedulers.Schedulers
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultSkipTimeRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultSkipTimeRepository

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = DefaultSkipTimeRepository(
            client = OkHttpClient(),
            anilistUrl = server.url("/graphql").toString(),
            aniSkipUrl = server.url("/aniskip").toString(),
            ioScheduler = Schedulers.trampoline()
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `uses known mal id directly and parses skip times`() {
        server.enqueue(
            MockResponse().setBody(
                """{"found":true,"results":[{"skipType":"op","interval":{"startTime":10.5,"endTime":120}}]}"""
            )
        )
        val anime = PlaybackAnime(aniListId = 21, malId = 12345, title = "AtsuLab Anime")
        val episode = PlaybackEpisode(name = "Episode 2", url = "ep-2", number = 2f)

        val intervals = repository.getSkipIntervals(anime, episode, durationMs = 1_425_000)
            .test().await().values().single()

        assertEquals(10500L to 120000L, intervals.single().startMs to intervals.single().endMs)
        val request = server.takeRequest()
        assertEquals(
            "/v2/skip-times/12345/2?types=op&types=ed&episodeLength=1425",
            request.path
        )
        assertEquals("Mozilla/5.0 (WATCH_APP) Chrome/120.0", request.getHeader("User-Agent"))
        assertEquals("application/json", request.getHeader("Accept"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `resolves cleaned title through anilist then caches mal id`() {
        server.enqueue(MockResponse().setBody("""{"data":{"Media":{"idMal":987}}}"""))
        server.enqueue(MockResponse().setBody("""{"found":false}"""))
        val anime = PlaybackAnime(aniListId = 21, title = "AtsuLab Anime (TV) Episode 3")

        val firstResult = repository.getSkipIntervals(anime, PlaybackEpisode("Episode 1", "ep-1", number = 1f), 1_440_000)
            .test().await().values().single()

        assertTrue(firstResult.isEmpty())
        val graphqlRequest = server.takeRequest()
        val skipRequest = server.takeRequest()
        assertEquals("/graphql", graphqlRequest.path)
        assertTrue(graphqlRequest.body.readUtf8().contains("\"search\":\"atsulab anime\""))
        assertEquals("/aniskip/v2/skip-times/987/1?types=op&types=ed&episodeLength=1440", skipRequest.path)

        server.enqueue(MockResponse().setBody("""{"found":false}"""))
        repository.getSkipIntervals(anime, PlaybackEpisode("Episode 2", "ep-2", number = 2f), 1_440_000)
            .test().await().values().single()

        assertEquals("/aniskip/v2/skip-times/987/2?types=op&types=ed&episodeLength=1440", server.takeRequest().path)
    }

    @Test
    fun `returns empty when aniskip responds unsuccessfully`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val anime = PlaybackAnime(aniListId = 21, malId = 12345, title = "AtsuLab Anime")

        val result = repository.getSkipIntervals(
            anime,
            PlaybackEpisode("Episode 1", "ep-1", number = 1f),
            1_440_000
        ).test().await().values().single()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `avoids network when title or episode is invalid`() {
        val blankTitle = PlaybackAnime(aniListId = 21, title = "")
        val zeroEpisode = PlaybackEpisode("Special", "special", number = 0f)

        assertTrue(repository.getSkipIntervals(blankTitle, zeroEpisode, 1000).test().await().values().single().isEmpty())
        assertEquals(0, server.requestCount)
    }
}
