package msr.atsulab.app.player.data.repository

import com.google.gson.JsonObject
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap
import msr.atsulab.app.player.data.provider.AniSkipResponseParser
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SkipInterval
import msr.atsulab.app.player.domain.repository.SkipTimeRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DefaultSkipTimeRepository(
    private val client: OkHttpClient,
    private val anilistUrl: String = ANILIST_URL,
    private val aniSkipUrl: String = ANISKIP_URL,
    private val ioScheduler: Scheduler = Schedulers.io(),
    private val malIdCache: MutableMap<String, Int> = ConcurrentHashMap()
) : SkipTimeRepository {

    override fun getSkipIntervals(
        anime: PlaybackAnime,
        episode: PlaybackEpisode,
        durationMs: Long
    ): Single<List<SkipInterval>> {
        return Single.fromCallable {
            if (episode.number.toInt() <= 0) return@fromCallable emptyList<SkipInterval>()
            val malId = anime.malId?.takeIf { it > 0 } ?: resolveMalId(anime.title)
            if (malId <= 0) return@fromCallable emptyList<SkipInterval>()

            val seconds = (durationMs / 1000).coerceAtLeast(0L)
            val url = "$aniSkipUrl/v2/skip-times/$malId/${episode.number.toInt()}" +
                "?types=op&types=ed&episodeLength=$seconds"
            val json = getText(buildRequest(url).get().build())
                ?: return@fromCallable emptyList<SkipInterval>()
            AniSkipResponseParser.parseIntervals(json)
        }.subscribeOn(ioScheduler)
    }

    private fun resolveMalId(title: String): Int {
        val cleanedTitle = cleanTitle(title)
        val cacheKey = cleanedTitle.lowercase()
        if (cacheKey.isEmpty()) return 0
        malIdCache[cacheKey]?.let { return it }

        val body = JsonObject().apply {
            addProperty("query", MAL_QUERY)
            add("variables", JsonObject().apply {
                addProperty("search", cacheKey)
            })
        }.toString()
        val request = buildRequest(anilistUrl)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val responseBody = getText(request) ?: ""
        val malId = AniSkipResponseParser.resolveMalId(responseBody).takeIf { it > 0 } ?: 0
        malIdCache[cacheKey] = malId
        return malId
    }

    private fun buildRequest(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header(USER_AGENT_HEADER, USER_AGENT)
            .header(ACCEPT_HEADER, ACCEPT_VALUE)
    }

    private fun getText(request: Request): String? {
        return client.newCall(request).execute().use { response ->
            response.body?.string()?.takeIf { response.isSuccessful }
        }
    }

    private fun cleanTitle(value: String?): String {
        return value.orEmpty()
            .replace(TITLE_SUFFIX_WITH_PARENTHESES_REGEX, "")
            .replace(TITLE_EPISODE_SUFFIX_REGEX, "")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    companion object {
        const val ANILIST_URL = "https://graphql.anilist.co"
        const val ANISKIP_URL = "https://api.aniskip.com"

        private const val USER_AGENT_HEADER = "User-Agent"
        private const val ACCEPT_HEADER = "Accept"
        private const val USER_AGENT = "Mozilla/5.0 (WATCH_APP) Chrome/120.0"
        private const val ACCEPT_VALUE = "application/json"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val MAL_QUERY =
            "query(${'$'}search:String){ Media(search:${'$'}search, type:ANIME){ idMal } }"
        private val TITLE_SUFFIX_WITH_PARENTHESES_REGEX =
            Regex("""\s*\([^)]*\)\s*$""")
        private val TITLE_EPISODE_SUFFIX_REGEX =
            Regex("""\s+Episode\s+\d+.*$""", RegexOption.IGNORE_CASE)
        private val WHITESPACE_REGEX = Regex("""\s+""")
    }
}
