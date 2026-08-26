package msr.atsulab.app.player.data.provider

import com.google.gson.JsonParser
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import java.net.URLEncoder
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.domain.provider.SourceProvider
import okhttp3.OkHttpClient
import okhttp3.Request

class AnifuxSourceProvider(
    private val client: OkHttpClient,
    private val baseUrl: String = BASE_URL,
    private val ioScheduler: Scheduler = Schedulers.io()
) : SourceProvider {

    override val id: String = PROVIDER_ID
    override val displayName: String = DISPLAY_NAME

    override fun findCandidates(
        title: String,
        aniListId: Int?
    ): Single<List<SourceCandidate>> {
        return findGroupedCandidates(title, aniListId).map(AnifuxResponseParser::flattenCandidateGroups)
    }

    override fun findCandidateGroups(
        title: String,
        aniListId: Int?
    ): Single<Map<String, List<SourceCandidate>>> {
        return Single.fromCallable {
            if (title.isBlank() && aniListId == null) return@fromCallable emptyMap<String, List<SourceCandidate>>()
            val encodedTitle = encode(title)
            val aniListQuery = aniListId?.toString()?.trim().orEmpty()
            var url = "$baseUrl/api/anime/source-candidates?title=$encodedTitle"
            if (aniListQuery.isNotEmpty()) {
                url += "&anilistId=${encode(aniListQuery)}"
            }
            AnifuxResponseParser.parseCandidateGroups(getText(url))
        }.subscribeOn(ioScheduler)
    }

    fun findGroupedCandidates(
        title: String,
        aniListId: Int?
    ): Single<Map<String, List<SourceCandidate>>> =
        findCandidateGroups(title, aniListId)

    override fun getEpisodes(candidate: SourceCandidate): Single<List<PlaybackEpisode>> {
        return Single.fromCallable {
            if (candidate.id.isBlank()) {
                throw IllegalArgumentException("Anifux candidate id is required")
            }
            val backendProvider = candidate.backendProvider.ifEmpty { AnifuxResponseParser.BACKEND_ANIDB }
            if (backendProvider != AnifuxResponseParser.BACKEND_ANIDB) {
                return@fromCallable emptyList<PlaybackEpisode>()
            }
            val json = getText("$baseUrl/api/anime/${encode(candidate.id)}/episodes")
            AnifuxResponseParser.parseEpisodes(
                candidate = candidate,
                json = json,
                confirmedAninekoSlug = candidate.confirmedSourceSlug.takeIf(String::isNotEmpty)
            )
        }.subscribeOn(ioScheduler)
    }

    override fun getSources(
        candidate: SourceCandidate,
        episode: PlaybackEpisode,
        preferredLanguage: String?
    ): Single<List<VideoSource>> {
        return Single.fromCallable {
            if (episode.url.isBlank()) {
                throw IllegalArgumentException("Anifux episode url is required")
            }
            var url = "$baseUrl/api/anime/episode/${encode(episode.url)}/sources" +
                "?title=${encode(episode.postTitle)}&ep=${episode.number.toInt()}"
            episode.confirmedSourceSlug?.takeIf(String::isNotEmpty)?.let { slug ->
                url += "&aninekoSlug=${encode(slug)}"
            }
            val sources = AnifuxResponseParser.parseSources(getText(url))
            if (sources.isEmpty()) throw IOException("No sources found")
            sources
        }.subscribeOn(ioScheduler)
    }

    fun resolveAnidbId(aniListId: String?, title: String?): Single<String> {
        return Single.fromCallable {
            val normalizedAniListId = aniListId?.trim().orEmpty()
            if (normalizedAniListId.isNotEmpty()) {
                runCatching {
                    val json = getText("$baseUrl/api/anime/resolve/aniList/${encode(normalizedAniListId)}")
                    AnifuxResponseParser.resolveAnidbId(json)
                }.getOrNull()?.takeIf(String::isNotEmpty)?.let { return@fromCallable it }
            }

            val normalizedTitle = title?.trim().orEmpty()
            if (normalizedTitle.isNotEmpty()) {
                runCatching {
                    val json = getText("$baseUrl/api/anime/search?q=${encode(normalizedTitle)}")
                    JsonParser.parseString(json).asJsonArray
                        .firstOrNull()
                        ?.takeIf { it.isJsonObject }
                        ?.asJsonObject
                        ?.get("id")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asString
                        .orEmpty()
                }.getOrNull()?.takeIf(String::isNotEmpty)?.let { return@fromCallable it }
            }

            normalizedAniListId.ifEmpty { title.orEmpty() }
        }.subscribeOn(ioScheduler)
    }

    private fun getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(USER_AGENT_HEADER, USER_AGENT)
            .header(ACCEPT_HEADER, ACCEPT_VALUE)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) throw IOException("HTTP ${response.code}")
            return body
        }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    companion object {
        const val PROVIDER_ID = "anifux"
        const val DISPLAY_NAME = "Anifux"
        const val BASE_URL = "https://anifux-zg01.onrender.com"

        private const val USER_AGENT_HEADER = "User-Agent"
        private const val ACCEPT_HEADER = "Accept"
        private const val USER_AGENT = "Anifux/1.0"
        private const val ACCEPT_VALUE = "application/json"
    }
}
