package msr.atsulab.app.player.data.provider

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.domain.provider.SourceProvider

class DakiSourceProvider(
    private val client: OkHttpClient,
    private val baseUrl: String = BASE_URL,
    private val ioScheduler: Scheduler = Schedulers.io()
) : SourceProvider {

    override val id: String = PROVIDER_ID
    override val displayName: String = DISPLAY_NAME

    override fun findCandidates(title: String, aniListId: Int?): Single<List<SourceCandidate>> {
        return Single.fromCallable {
            aniListId?.let { resolvedId ->
                resolveDirectly(resolvedId.toString())?.let { directId ->
                    return@fromCallable listOf(SourceCandidate(id = directId, title = title))
                }
            }
            if (title.isBlank()) {
                aniListId?.let { resolvedId ->
                    return@fromCallable listOf(
                        SourceCandidate(id = resolvedId.toString(), title = title)
                    )
                }
                return@fromCallable emptyList()
            }
            searchCandidates(title)
        }.subscribeOn(ioScheduler)
    }

    override fun getEpisodes(candidate: SourceCandidate): Single<List<PlaybackEpisode>> {
        return Single.fromCallable {
            if (candidate.id.isBlank()) {
                throw IllegalArgumentException("Daki candidate id is required")
            }
            val json = getText("$baseUrl/api/frontend/anime/${candidate.id}/episodes")
            DakiResponseParser.parseEpisodes(candidateId = candidate.id, json = json)
        }.subscribeOn(ioScheduler)
    }

    override fun getSources(
        candidate: SourceCandidate,
        episode: PlaybackEpisode,
        preferredLanguage: String?
    ): Single<List<VideoSource>> {
        return Single.fromCallable {
            if (candidate.id.isBlank()) {
                throw IllegalArgumentException("Daki candidate id is required")
            }
            val episodes = getEpisodesBlocking(candidate.id)
            val targetEpisode = DakiResponseParser.selectEpisode(episodes, episode.number.toInt())
            val json = getText("$baseUrl/api/frontend/episode/${targetEpisode.url}/languages")
            DakiResponseParser.parseSources(json) { embedUrl -> getText(embedUrl) }
        }.subscribeOn(ioScheduler)
    }

    private fun searchCandidates(query: String): List<SourceCandidate> {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val html = getText("$baseUrl/browse?q=$encodedQuery")
        return DakiResponseParser
            .rankSearchResults(DakiResponseParser.parseSearchResults(html), query)
            .map { result ->
                SourceCandidate(
                    id = result.id,
                    title = result.title,
                    thumbnailUrl = result.thumbnailUrl
                )
            }
    }

    private fun resolveDirectly(aniListId: String): String? {
        return try {
            val encodedId = URLEncoder.encode(aniListId.trim(), Charsets.UTF_8.name())
            val json = getText("$baseUrl/api/frontend/resolve?anilistId=$encodedId")
            DakiResponseParser.resolveAnidbId(json).ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun getEpisodesBlocking(anidbId: String): List<PlaybackEpisode> {
        val json = getText("$baseUrl/api/frontend/anime/$anidbId/episodes")
        return DakiResponseParser.parseEpisodes(candidateId = anidbId, json = json)
    }

    private fun getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(USER_AGENT_HEADER, USER_AGENT)
            .header(ACCEPT_HEADER, ACCEPT_VALUE)
            .header(REFERER_HEADER, "$baseUrl/")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw IOException("HTTP ${response.code} $url")
            }
            if (body.contains(CLOUDFLARE_CHALLENGE_MARKER) && body.contains(CLOUDFLARE_OPTION_MARKER)) {
                throw IOException("CF_CHALLENGE $url")
            }
            return body
        }
    }

    companion object {
        const val PROVIDER_ID = "daki"
        const val DISPLAY_NAME = "Daki"
        const val BASE_URL = "https://anidb.app"

        private const val USER_AGENT_HEADER = "User-Agent"
        private const val ACCEPT_HEADER = "Accept"
        private const val REFERER_HEADER = "Referer"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/125 Mobile Safari/537.36"
        private const val ACCEPT_VALUE = "text/html,application/json,*/*"
        private const val CLOUDFLARE_CHALLENGE_MARKER = "Just a moment"
        private const val CLOUDFLARE_OPTION_MARKER = "_cf_chl_opt"
    }
}
