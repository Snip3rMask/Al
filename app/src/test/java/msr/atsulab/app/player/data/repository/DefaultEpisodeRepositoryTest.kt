package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultEpisodeRepositoryTest {

    private val anime = PlaybackAnime(aniListId = 21, title = "AtsuLab Anime", coverImageUrl = "cover.jpg")

    @Test
    fun `falls across failed and empty providers then tags successful metadata`() {
        val failed = FakeSourceProvider("mkissa", candidateError = true)
        val empty = FakeSourceProvider("anifux")
        val success = FakeSourceProvider(
            id = "daki",
            episodes = listOf(PlaybackEpisode(name = "Episode 1", url = "ep-1", number = 1f))
        )
        val repository = DefaultEpisodeRepository(listOf(failed, empty, success))

        val episodes = repository.getEpisodes(anime).test().await().values().single()

        assertEquals(1, failed.candidateCalls)
        assertEquals(1, empty.candidateCalls)
        assertEquals(1, empty.episodeCalls)
        assertEquals(1, success.episodeCalls)
        assertEquals("daki", episodes.single().providerId)
        assertEquals("cover.jpg", episodes.single().thumbnailUrl)
        assertEquals("AtsuLab Anime", episodes.single().postTitle)
        assertEquals(21, episodes.single().aniListId)
    }

    @Test
    fun `anifux prefers compatible anidb backend candidate`() {
        val anifux = FakeSourceProvider(
            id = "anifux",
            candidates = listOf(
                SourceCandidate(id = "nora-slug", title = "Nora", backendProvider = "anineko"),
                SourceCandidate(id = "123", title = "Daki", backendProvider = "anidb")
            ),
            episodes = listOf(PlaybackEpisode(name = "Episode 2", url = "ep-2", number = 2f))
        )
        val daki = FakeSourceProvider(id = "daki", episodes = listOf(PlaybackEpisode("Fallback", "fallback")))
        val repository = DefaultEpisodeRepository(listOf(anifux, daki))

        val result = repository.getEpisodes(anime).test().await().values().single()

        assertEquals(1, anifux.episodeCalls)
        assertEquals(0, daki.candidateCalls)
        assertEquals("ep-2", result.single().url)
    }

    @Test
    fun `returns empty when every provider is unavailable`() {
        val failed = FakeSourceProvider("mkissa", candidateError = true)
        val emptyCandidates = FakeSourceProvider("anifux", candidates = emptyList())
        val emptyEpisodes = FakeSourceProvider("daki", episodes = emptyList())
        val repository = DefaultEpisodeRepository(listOf(failed, emptyCandidates, emptyEpisodes))

        val result = repository.getEpisodes(anime).test().await().values().single()

        assertEquals(emptyList<PlaybackEpisode>(), result)
    }

    @Test
    fun `supports deferred failure semantics`() {
        val failed = FakeSourceProvider("mkissa", candidateError = true)
        val repository = DefaultEpisodeRepository(listOf(failed))
        val single: Single<List<PlaybackEpisode>> = repository.getEpisodes(anime)

        assertEquals(emptyList<PlaybackEpisode>(), single.test().await().values().single())
    }
}
