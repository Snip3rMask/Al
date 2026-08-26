package msr.atsulab.app.player.data.repository

import msr.atsulab.app.player.domain.model.SourceCandidate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultSourceCandidateRepositoryTest {

    @Test
    fun `merges matching provider sections and removes duplicate candidate ids`() {
        val daki = FakeSourceProvider(
            id = "daki",
            displayName = "Daki",
            candidates = listOf(
                SourceCandidate(id = "anidb-1", title = "AtsuLab Anime", backendProvider = "anidb"),
                SourceCandidate(id = "anidb-2", title = "AtsuLab Anime II", backendProvider = "anidb")
            )
        )
        val anifux = FakeSourceProvider(
            id = "anifux",
            displayName = "Daki",
            candidates = listOf(
                SourceCandidate(id = "anidb-1", title = "Duplicate", backendProvider = "anidb"),
                SourceCandidate(id = "anineko-1", title = "AtsuLab Anime", backendProvider = "anineko")
            )
        )

        val sections = DefaultSourceCandidateRepository(listOf(daki, anifux))
            .findCandidates(title = "AtsuLab Anime", aniListId = 21)
            .test().await().values().single()

        assertEquals(1, sections.size)
        val section = sections.single()
        assertEquals("daki", section.providerId)
        assertEquals("Daki", section.displayName)
        assertEquals(listOf("anidb-1", "anidb-2", "anineko-1"), section.candidates.map { it.id })
    }

    @Test
    fun `keeps successful providers when another provider fails`() {
        val failed = FakeSourceProvider(id = "daki", displayName = "Daki", candidateError = true)
        val anineko = FakeSourceProvider(
            id = "anifux-backend",
            displayName = "Nora",
            candidates = listOf(SourceCandidate(id = "nora-1", title = "AtsuLab Anime"))
        )

        val sections = DefaultSourceCandidateRepository(listOf(failed, anineko))
            .findCandidates(title = "AtsuLab Anime", aniListId = null)
            .test().await().values().single()

        assertEquals("Nora", sections.single().displayName)
        assertEquals(1, failed.candidateCalls)
    }

    @Test
    fun `errors when every requested provider fails`() {
        val first = FakeSourceProvider(id = "daki", candidateError = true)
        val second = FakeSourceProvider(id = "mkissa", candidateError = true)

        DefaultSourceCandidateRepository(listOf(first, second))
            .findCandidates(title = "AtsuLab Anime", aniListId = 21)
            .test().await().assertError(IllegalStateException::class.java)
    }

    @Test
    fun `single server type filters by display or provider name`() {
        val daki = FakeSourceProvider(
            id = "daki",
            displayName = "Daki",
            candidates = listOf(SourceCandidate(id = "daki-1", title = "AtsuLab Anime"))
        )
        val nora = FakeSourceProvider(
            id = "anifux-backend",
            displayName = "Nora",
            candidates = listOf(SourceCandidate(id = "nora-1", title = "AtsuLab Anime"))
        )
        val repository = DefaultSourceCandidateRepository(listOf(daki, nora))

        val byDisplay = repository.findCandidates("AtsuLab Anime", null, "Nora")
            .test().await().values().single()
        val byProvider = repository.findCandidates("AtsuLab Anime", null, "daki")
            .test().await().values().single()

        assertEquals(listOf("Nora"), byDisplay.map { it.displayName })
        assertEquals(listOf("Daki"), byProvider.map { it.displayName })
    }
}
