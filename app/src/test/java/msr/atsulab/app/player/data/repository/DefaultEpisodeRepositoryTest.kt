package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.storage.SourceMapping
import msr.atsulab.app.player.storage.SourceMappingStore
import msr.atsulab.app.player.storage.SourcePick
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultEpisodeRepositoryTest {

    private class RecordingSourceMappingStore : SourceMappingStore {
        var mapping: SourceMapping? = null

        override fun get(aniListId: String): SourceMapping? = mapping
        override fun save(mapping: SourceMapping) { this.mapping = mapping }
        override fun has(aniListId: String): Boolean = mapping?.picks?.isNotEmpty() == true
        override fun clear(aniListId: String) { this.mapping = null }
    }


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
    fun `uses confirmed daki id and nora source slug during episode resolution`() {
        val groups = linkedMapOf(
            "Daki" to listOf(
                SourceCandidate(id = "wrong-anidb", title = "Wrong", backendProvider = "anidb"),
                SourceCandidate(id = "correct-anidb", title = "Correct", backendProvider = "anidb")
            ),
            "Nora" to listOf(SourceCandidate(id = "confirmed-slug", title = "Nora"))
        )
        val anifux = FakeSourceProvider(id = "anifux", candidateGroups = groups, episodes = listOf(episode()))
        val store = RecordingSourceMappingStore().apply {
            mapping = SourceMapping(
                aniListId = "21",
                picks = mapOf(
                    "Daki" to SourcePick("correct-anidb", "Correct"),
                    "Nora" to SourcePick("confirmed-slug", "Nora")
                )
            )
        }
        val repository = DefaultEpisodeRepository(listOf(anifux), sourceMappingStore = store)

        val result = repository.getEpisodes(anime).test().await().values().single()

        assertEquals("correct-anidb", anifux.requestedCandidates.single().id)
        assertEquals("confirmed-slug", anifux.requestedCandidates.single().confirmedSourceSlug)
        assertEquals(1, result.size)
    }

    @Test
    fun `unmapped providers do not preempt a confirmed primary mapping`() {
        val mkissa = FakeSourceProvider(id = "mkissa", episodes = listOf(episode(url = "wrong-auto-match")))
        val groups = linkedMapOf(
            "Daki" to listOf(SourceCandidate(id = "confirmed-anidb", title = "Correct", backendProvider = "anidb")),
            "Nora" to listOf(SourceCandidate(id = "confirmed-slug", title = "Nora"))
        )
        val anifux = FakeSourceProvider(id = "anifux", candidateGroups = groups, episodes = listOf(episode()))
        val store = RecordingSourceMappingStore().apply {
            mapping = SourceMapping(
                aniListId = "21",
                picks = mapOf("Daki" to SourcePick("confirmed-anidb", "Correct"))
            )
        }
        val repository = DefaultEpisodeRepository(listOf(mkissa, anifux), sourceMappingStore = store)

        val result = repository.getEpisodes(anime).test().await().values().single()

        assertEquals(0, mkissa.episodeCalls)
        assertEquals("confirmed-anidb", anifux.requestedCandidates.single().id)
        assertEquals(1, result.size)
    }

    @Test
    fun `stale confirmed primary does not silently select another same-server match`() {
        val mapped = FakeSourceProvider(
            id = "anifux",
            candidateGroups = linkedMapOf(
                "Daki" to listOf(SourceCandidate(id = "current-wrong", title = "Wrong", backendProvider = "anidb"))
            ),
            episodes = listOf(episode())
        )
        val fallback = FakeSourceProvider(
            id = "daki",
            displayName = "Daki",
            candidateGroups = linkedMapOf(
                "Daki" to listOf(SourceCandidate(id = "correct-direct", title = "Correct"))
            ),
            episodes = listOf(episode(url = "fallback-episode"))
        )
        val store = RecordingSourceMappingStore().apply {
            mapping = SourceMapping(
                aniListId = "21",
                picks = mapOf("Daki" to SourcePick("removed-anidb", "Removed"))
            )
        }
        val repository = DefaultEpisodeRepository(listOf(mapped, fallback), sourceMappingStore = store)

        val result = repository.getEpisodes(anime).test().await().values().single()

        assertEquals(0, mapped.episodeCalls)
        assertEquals(0, fallback.episodeCalls)
        assertEquals(emptyList<PlaybackEpisode>(), result)
    }

    @Test
    fun `skipped primary section prevents that provider candidate from playing`() {
        val mapped = FakeSourceProvider(
            id = "anifux",
            candidateGroups = linkedMapOf(
                "Daki" to listOf(SourceCandidate(id = "anidb-id", title = "Wrong", backendProvider = "anidb")),
                "Nora" to listOf(SourceCandidate(id = "nora-slug", title = "Nora"))
            ),
            episodes = listOf(episode())
        )
        val fallback = FakeSourceProvider(id = "deki-fallback", displayName = "Fallback", episodes = emptyList())
        val store = RecordingSourceMappingStore().apply {
            mapping = SourceMapping(aniListId = "21", skipped = setOf("Daki"))
        }
        val repository = DefaultEpisodeRepository(listOf(mapped, fallback), sourceMappingStore = store)

        val result = repository.getEpisodes(anime).test().await().values().single()

        assertEquals(0, mapped.episodeCalls)
        assertEquals(emptyList<PlaybackEpisode>(), result)
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

    @Test
    fun `reports failed and empty fallback providers without identifiers leaking into messages`() {
        val diagnostics = RecordingPlaybackDiagnostics()
        val failed = FakeSourceProvider("mkissa", candidateError = true)
        val empty = FakeSourceProvider("anifux")
        val success = FakeSourceProvider(
            id = "daki",
            episodes = listOf(PlaybackEpisode(name = "Episode 1", url = "ep-1", number = 1f))
        )
        val repository = DefaultEpisodeRepository(listOf(failed, empty, success), diagnostics)

        repository.getEpisodes(anime).test().await().values().single()

        assertEquals(listOf("episode:mkissa:0:21:IllegalStateException"), diagnostics.failures)
        assertEquals(listOf("episode:anifux:1:21:no-episodes"), diagnostics.skipped)
    }
}

private fun episode(name: String = "Episode 1", url: String = "ep-1") = PlaybackEpisode(
    name = name,
    url = url,
    number = 1f
)
