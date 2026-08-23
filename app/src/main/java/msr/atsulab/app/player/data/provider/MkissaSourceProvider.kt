package msr.atsulab.app.player.data.provider

import com.google.gson.JsonObject
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.domain.provider.SourceProvider

class MkissaSourceProvider(
    private val client: OkHttpClient,
    private val apiUrl: String = API_URL,
    private val siteOrigin: String = SITE_ORIGIN,
    private val ioScheduler: Scheduler = Schedulers.io()
) : SourceProvider {

    override val id: String = PROVIDER_ID
    override val displayName: String = DISPLAY_NAME

    override fun findCandidates(title: String, aniListId: Int?): Single<List<SourceCandidate>> {
        return Single.fromCallable {
            aniListId?.let { resolvedId ->
                listOf(SourceCandidate(id = resolvedId.toString(), title = title))
            } ?: emptyList()
        }.subscribeOn(ioScheduler)
    }

    override fun getEpisodes(candidate: SourceCandidate): Single<List<PlaybackEpisode>> {
        return Single.fromCallable {
            val aniListId = candidate.id.trim()
            if (aniListId.isEmpty()) throw IllegalArgumentException("Mkissa AniList id is required")

            val payload = JsonObject().apply {
                addProperty("query", EPISODES_QUERY)
                add("variables", JsonObject().apply {
                    addProperty("_id", aniListId)
                })
            }.toString()

            val request = Request.Builder()
                .url(apiUrl)
                .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE)
                .header(ORIGIN_HEADER, siteOrigin)
                .header(REFERER_HEADER, "$siteOrigin/")
                .header(USER_AGENT_HEADER, USER_AGENT)
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    throw IOException("HTTP ${response.code}")
                }
                MkissaResponseParser.parseEpisodes(request = candidate, json = body)
            }
        }.subscribeOn(ioScheduler)
    }

    override fun getSources(
        candidate: SourceCandidate,
        episode: PlaybackEpisode,
        preferredLanguage: String?
    ): Single<List<VideoSource>> {
        return Single.just(emptyList<VideoSource>()).subscribeOn(ioScheduler)
    }

    companion object {
        const val PROVIDER_ID = "mkissa"
        const val DISPLAY_NAME = "Mkissa"
        const val API_URL = "https://api.mkissa.net/api"
        const val SITE_ORIGIN = "https://mkissa.to"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val ORIGIN_HEADER = "Origin"
        private const val REFERER_HEADER = "Referer"
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val CONTENT_TYPE_VALUE = "application/json"
        private const val USER_AGENT = "Mozilla/5.0"
        private val EPISODES_QUERY =
            "query (${'$'}_id: String!) { show(_id: ${'$'}_id) { _id name aniListId malId availableEpisodes availableEpisodesDetail } }"
    }
}
