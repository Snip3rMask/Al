package msr.atsulab.app.ui.search.storage

import msr.atsulab.app.helper.enums.SearchCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecentSearchNormalizationTest {

    @Test
    fun `deduplicates case insensitive queries within same category and ranks newest first`() {
        val updated = normalizeRecentSearches(
            existing = listOf(
                RecentSearch("  AtsuLab ", SearchCategory.ANIME, 1_000L),
                RecentSearch("Old", SearchCategory.ANIME, 3_000L)
            ),
            incoming = RecentSearch("atsulab", SearchCategory.ANIME, 2_000L)
        )

        assertEquals(listOf("Old", "atsulab"), updated.map(RecentSearch::query))
        assertEquals(listOf(3_000L, 2_000L), updated.map(RecentSearch::updatedAtEpochMs))
    }

    @Test
    fun `keeps identical query for different search categories`() {
        val updated = normalizeRecentSearches(
            existing = listOf(RecentSearch("AtsuLab", SearchCategory.MANGA, 1_000L)),
            incoming = RecentSearch("AtsuLab", SearchCategory.ANIME, 2_000L)
        )

        assertEquals(SearchCategory.ANIME, updated.first().category)
        assertEquals(SearchCategory.MANGA, updated.last().category)
    }

    @Test
    fun `removes blank entries and enforces limit`() {
        val existing = (1..15).map { RecentSearch("Query $it", SearchCategory.ANIME, it.toLong()) } +
            RecentSearch("   ", SearchCategory.ANIME, 100L)

        val updated = normalizeRecentSearches(existing, RecentSearch("", SearchCategory.MANGA, 999L))

        assertEquals(RECENT_SEARCH_LIMIT, updated.size)
        assertTrue(updated.none { it.query.isBlank() })
        assertEquals("Query 15", updated.first().query)
        assertEquals("Query 4", updated.last().query)
    }
}
