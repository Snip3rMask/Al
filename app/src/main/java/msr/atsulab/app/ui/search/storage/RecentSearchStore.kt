package msr.atsulab.app.ui.search.storage

import android.content.Context
import com.google.gson.Gson
import msr.atsulab.app.helper.enums.SearchCategory

data class RecentSearch(
    val query: String,
    val category: SearchCategory,
    val updatedAtEpochMs: Long
)

interface RecentSearchStore {
    fun all(): List<RecentSearch>

    fun add(search: RecentSearch): List<RecentSearch>

    fun remove(search: RecentSearch): List<RecentSearch>

    fun clear(): List<RecentSearch>
}

internal fun normalizeRecentSearches(
    existing: List<RecentSearch>,
    incoming: RecentSearch,
    limit: Int = RECENT_SEARCH_LIMIT
): List<RecentSearch> {
    val query = incoming.query.trim()
    if (query.isEmpty() || limit <= 0) {
        return existing.filter { it.query.isNotBlank() }
            .sortedByDescending(RecentSearch::updatedAtEpochMs)
            .take(limit.coerceAtLeast(0))
    }
    val normalizedIncoming = incoming.copy(query = query)
    return (listOf(normalizedIncoming) + existing).distinctBy {
        it.query.trim().lowercase() to it.category
    }.filter { it.query.isNotBlank() }.sortedByDescending(RecentSearch::updatedAtEpochMs).take(limit)
}

class DefaultRecentSearchStore(
    context: Context,
    private val gson: Gson = Gson(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : RecentSearchStore {

    private val preferences by lazy {
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun all(): List<RecentSearch> {
        return synchronized(this) { readLocked() }
    }

    override fun add(search: RecentSearch): List<RecentSearch> {
        return synchronized(this) {
            val updated = normalizeRecentSearches(readLocked(), search.copy(updatedAtEpochMs = currentTimeMillis()))
            writeLocked(updated)
        }
    }

    override fun remove(search: RecentSearch): List<RecentSearch> {
        return synchronized(this) {
            writeLocked(readLocked().filterNot { it == search })
        }
    }

    override fun clear(): List<RecentSearch> {
        return synchronized(this) { writeLocked(emptyList()) }
    }

    private fun readLocked(): List<RecentSearch> {
        val raw = preferences.getString(SEARCHES_KEY, null) ?: return emptyList()
        return try {
            gson.fromJson(raw, Array<RecentSearch>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeLocked(searches: List<RecentSearch>): List<RecentSearch> {
        preferences.edit().putString(SEARCHES_KEY, gson.toJson(searches)).apply()
        return searches.sortedByDescending(RecentSearch::updatedAtEpochMs)
    }

    private companion object {
        const val PREFERENCES_NAME = "atsu_recent_searches"
        const val SEARCHES_KEY = "searches"
    }
}

internal const val RECENT_SEARCH_LIMIT = 12
