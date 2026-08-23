package msr.atsulab.app.player.data.provider

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource
import org.jsoup.Jsoup
import kotlin.math.roundToInt

internal object DakiResponseParser {

    private val HLS_REGEX = Regex("https://hls\\.anidb\\.app/stream/[^'\"\\s]+?/master\\.m3u8")
    private val ANIME_ID_REGEX = Regex("-(\\d+)$")

    fun extractAnimeId(url: String?): String {
        return ANIME_ID_REGEX.find(url.orEmpty())?.groupValues?.get(1).orEmpty()
    }

    fun parseSearchResults(html: String): List<SearchResult> {
        val document = Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()

        for (anchor in document.select("a[href*=\"/anime/\"]")) {
            if (results.size >= MAX_SEARCH_RESULTS) break

            val href = anchor.attr("href")
            val image = anchor.selectFirst("img")
            var title = anchor.attr("title")
            if (title.isEmpty()) title = image?.attr("alt") ?: ""
            if (title.isEmpty()) title = anchor.text().trim()

            val thumbnail = image?.attr("src").orEmpty()
            val animeId = extractAnimeId(href)
            if (title.isNotBlank() && animeId.isNotEmpty()) {
                results.add(
                    SearchResult(
                        id = animeId,
                        title = title.trim(),
                        thumbnailUrl = absoluteThumbnailUrl(thumbnail),
                        url = absoluteUrl(href)
                    )
                )
            }
        }

        return results
    }

    fun rankSearchResults(results: List<SearchResult>, query: String): List<SearchResult> {
        val normalizedQuery = normalize(query)
        return results
            .map { result -> ScoredSearchResult(result, score(normalizedQuery, normalize(result.title))) }
            .sortedByDescending(ScoredSearchResult::score)
            .map(ScoredSearchResult::result)
    }

    fun resolveAnidbId(json: String): String {
        val root = parseObject(json)
        return sequenceOf("anidbId", "id", "anidb_id")
            .map { key -> root.optString(key).trim() }
            .firstOrNull(String::isNotEmpty)
            .orEmpty()
    }

    fun parseEpisodes(candidateId: String, json: String): List<PlaybackEpisode> {
        val episodes = parseObject(json).optJsonArray("episodes")
        return buildList {
            for (index in 0 until episodes.size()) {
                val episode = episodes.optJsonObject(index) ?: continue
                val episodeId = episode.optString("id")
                val number = episode.optDouble("number", (index + 1).toDouble())
                add(
                    PlaybackEpisode(
                        name = "Episode ${number.toInt()}",
                        url = episodeId,
                        thumbnailUrl = episode.optString("thumbnail"),
                        number = number.toFloat(),
                        playbackId = candidateId,
                        confirmedSourceSlug = DakiSourceProvider.PROVIDER_ID
                    )
                )
            }
        }
    }

    fun selectEpisode(episodes: List<PlaybackEpisode>, requestedNumber: Int): PlaybackEpisode {
        val exactMatch = episodes.firstOrNull { episode -> episode.number.toInt() == requestedNumber }
        if (exactMatch != null) return requireValidEpisode(exactMatch, requestedNumber)

        val fallbackIndex = requestedNumber - 1
        if (episodes.isNotEmpty() && fallbackIndex >= 0 && fallbackIndex < episodes.size) {
            return requireValidEpisode(episodes[fallbackIndex], requestedNumber)
        }

        throw IllegalArgumentException("Episode not found for episode $requestedNumber")
    }

    fun parseSources(json: String, htmlProvider: (String) -> String): List<VideoSource> {
        val languages = parseObject(json).optJsonArray("languages")
        if (languages.size() == 0) throw IllegalStateException("No languages")

        val sources = mutableListOf<VideoSource>()
        for (index in 0 until languages.size()) {
            val language = languages.optJsonObject(index) ?: continue
            val embedUrl = language.optString("embed_url")
            if (embedUrl.isEmpty()) continue

            try {
                val hlsUrl = HLS_REGEX.find(htmlProvider(embedUrl))?.value ?: continue
                val name = sequenceOf("name", "code")
                    .map { key -> language.optString(key) }
                    .firstOrNull(String::isNotEmpty) ?: DEFAULT_SOURCE_NAME
                sources.add(createSource(name, hlsUrl))
            } catch (_: Exception) {
                continue
            }
        }

        if (sources.isEmpty()) throw IllegalStateException("No Daki sources")
        return sources
    }

    internal fun createSource(name: String, url: String): VideoSource {
        val lowercaseName = name.lowercase()
        val isDub = !lowercaseName.contains("japan") &&
            !lowercaseName.contains("jpn") &&
            (lowercaseName.contains("dub") || lowercaseName.contains("english"))

        return VideoSource(
            quality = name,
            url = url,
            language = if (isDub) LANGUAGE_DUB else LANGUAGE_SUB,
            server = "Daki • $name",
            legacySourceId = "primary",
            displayName = "Daki",
            referer = DakiSourceProvider.BASE_URL + "/"
        )
    }

    private fun parseObject(json: String): JsonObject {
        return JsonParser.parseString(json).asJsonObject
    }

    private fun absoluteThumbnailUrl(url: String): String {
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.isEmpty() -> ""
            else -> "https:$url"
        }
    }

    private fun absoluteUrl(url: String): String {
        return if (url.startsWith("http")) url else DakiSourceProvider.BASE_URL + url
    }

    private fun JsonObject.optString(name: String): String {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive) ?: return ""
        return value.asJsonPrimitive.asString
    }

    private fun JsonObject.optDouble(name: String, defaultValue: Double): Double {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive) ?: return defaultValue
        return runCatching(value::getAsDouble).getOrDefault(defaultValue)
    }

    private fun JsonObject.optJsonArray(name: String): JsonArray {
        return get(name)?.takeIf(JsonElement::isJsonArray)?.asJsonArray ?: JsonArray()
    }

    private fun JsonArray.optJsonObject(index: Int): JsonObject? {
        return get(index)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
    }

    private fun normalize(value: String): String {
        return value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    }

    internal fun score(query: String, candidate: String): Int {
        if (query == candidate) return EXACT_SCORE
        if (query.contains(candidate) || candidate.contains(query)) return CONTAINS_SCORE

        val queryWords = query.split(Regex("\\s+")).filter(String::isNotEmpty)
        val candidateWords = candidate.split(Regex("\\s+")).filter(String::isNotEmpty)
        if (queryWords.isEmpty() || candidateWords.isEmpty()) return NO_SCORE

        val candidateWordSet = candidateWords.toSet()
        val commonWords = queryWords.count(candidateWordSet::contains)
        return (commonWords / maxOf(queryWords.size, candidateWords.size).toDouble() * WORD_MATCH_SCORE)
            .roundToInt()
    }

    private fun requireValidEpisode(episode: PlaybackEpisode, requestedNumber: Int): PlaybackEpisode {
        if (episode.url.isBlank()) {
            throw IllegalArgumentException("Episode not found for episode $requestedNumber")
        }
        return episode
    }

    internal data class SearchResult(
        val id: String,
        val title: String,
        val thumbnailUrl: String,
        val url: String
    )

    private data class ScoredSearchResult(
        val result: SearchResult,
        val score: Int
    )

    private const val MAX_SEARCH_RESULTS = 20
    private const val DEFAULT_SOURCE_NAME = "Source"
    private const val LANGUAGE_DUB = "Dub"
    private const val LANGUAGE_SUB = "Sub"
    private const val EXACT_SCORE = 100
    private const val CONTAINS_SCORE = 85
    private const val WORD_MATCH_SCORE = 60.0
    private const val NO_SCORE = 0
}
