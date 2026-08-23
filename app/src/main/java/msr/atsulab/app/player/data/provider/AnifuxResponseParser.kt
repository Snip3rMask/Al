package msr.atsulab.app.player.data.provider

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLDecoder
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SkipInterval
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource

internal object AnifuxResponseParser {

    val providerNames = linkedMapOf(
        "anidb" to "Daki",
        "anineko" to "Nora",
        "dhive" to "Hina",
        "anikoto" to "Kira",
        "anizone" to "Zuki",
        "animegg" to "Gura",
        "reanime" to "Rika",
        "allmanga" to "Miku"
    )

    fun parseCandidateGroups(json: String): Map<String, List<SourceCandidate>> {
        val root = json.asJsonObjectOrNull() ?: return emptyMap()
        return buildMap {
            providerNames.forEach { (backendKey, displayName) ->
                val candidates = root.optJsonArray(backendKey).parseCandidates(backendKey)
                if (candidates.isNotEmpty()) put(displayName, candidates)
            }
        }
    }

    fun flattenCandidateGroups(groups: Map<String, List<SourceCandidate>>): List<SourceCandidate> {
        return groups.values.flatten()
    }

    fun parseEpisodes(
        candidate: SourceCandidate,
        json: String,
        confirmedAninekoSlug: String? = null
    ): List<PlaybackEpisode> {
        val array = json.asJsonArrayOrNull() ?: return emptyList()
        return array.mapIndexedNotNull { index, element ->
            val episode = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val number = episode.optDouble("number", (index + 1).toDouble()).toFloat()
            PlaybackEpisode(
                name = "Episode ${number.toInt()}",
                url = episode.optString("id"),
                thumbnailUrl = candidate.thumbnailUrl,
                number = number,
                postTitle = candidate.title,
                playbackId = candidate.id,
                confirmedSourceSlug = confirmedAninekoSlug?.takeIf(String::isNotEmpty)
                    ?.takeIf { candidate.backendProvider == BACKEND_ANINEKO }
            )
        }
    }

    fun resolveAnidbId(json: String): String {
        return json.asJsonObjectOrNull()?.optString("anidbId").orEmpty()
    }

    fun parseSources(json: String): List<VideoSource> {
        val array = json.asJsonArrayOrNull() ?: return emptyList()
        return array.mapNotNull { element -> element.asJsonObjectOrNull() }.map { source ->
            var subtitleUrl = extractSubtitle(source)
            val directIntro = readSkip(source, "intro", "introStart", "introEnd")
            val directOutro = readSkip(source, "outro", "outroStart", "outroEnd")
            val skips = mutableListOf<SkipInterval>()
            directIntro?.let(skips::add)
            directOutro?.let(skips::add)

            val nestedSkips = source.optJsonArray("skipTimes")
            if (directIntro == null || directOutro == null) {
                nestedSkips.mapNotNull { element -> element.asJsonObjectOrNull() }.forEach { skip ->
                    val type = skip.optString("type").ifEmpty { skip.optString("skipType") }
                    val interval = SkipInterval(
                        startMs = skip.optSeconds("startTime", "start").toMillis(),
                        endMs = skip.optSeconds("endTime", "end").toMillis(),
                        type = type.ifEmpty { SkipInterval.DEFAULT_TYPE }
                    )
                    if (type.contains("op", ignoreCase = true) && directIntro == null) {
                        skips.add(interval)
                    }
                    if (type.contains("ed", ignoreCase = true) && directOutro == null) {
                        skips.add(interval)
                    }
                }
            }

            if (subtitleUrl.isEmpty()) {
                val caption = source.optString("caption_1")
                if (caption.contains(".vtt")) subtitleUrl = caption
            }
            subtitleUrl = decodeCaptionSubtitle(subtitleUrl)
            if (subtitleUrl.isEmpty()) {
                subtitleUrl = decodeCaptionSubtitle(source.optString("url"))
            }

            VideoSource(
                quality = source.optString("label").ifEmpty { "Source" },
                url = source.optString("url"),
                language = source.optString("language").ifEmpty { "Sub" },
                server = source.optString("server").ifEmpty { "Server" },
                legacySourceId = "primary",
                subtitleUrl = subtitleUrl,
                displayName = source.optString("displayName"),
                referer = source.optString("referer"),
                skipIntervals = skips.distinctBy { it.startMs to it.endMs to it.type }
            )
        }
    }

    private fun JsonArray.parseCandidates(backendProvider: String): List<SourceCandidate> {
        return mapNotNull { element -> element.asJsonObjectOrNull() }
            .map { candidate ->
                SourceCandidate(
                    id = candidate.optString("id"),
                    title = candidate.optString("title"),
                    thumbnailUrl = candidate.optString("thumbnail"),
                    backendProvider = backendProvider
                )
            }
            .filter { it.id.isNotEmpty() && it.title.isNotEmpty() }
    }

    private fun extractSubtitle(source: JsonObject): String {
        source.optString("subtitleUrl").let { value -> if (value.isNotEmpty()) return value }
        val subtitles = source.optJsonArray("subtitles")
        val first = subtitles.firstOrNull()?.asJsonObjectOrNull() ?: return ""
        return first.optString("url").ifEmpty { first.optString("src") }
    }

    private fun decodeCaptionSubtitle(value: String): String {
        if (!value.contains("caption_1=")) return value
        return runCatching {
            val decoded = URLDecoder.decode(value, Charsets.UTF_8.name())
            val start = decoded.indexOf(CAPTION_QUERY_KEY)
            if (start < 0) return ""
            var subtitle = decoded.substring(start + CAPTION_QUERY_KEY.length)
            val ampersand = subtitle.indexOf('&')
            if (ampersand >= 0) subtitle = subtitle.substring(0, ampersand)
            subtitle.takeIf { it.contains(".vtt") }.orEmpty()
        }.getOrDefault("")
    }

    private fun readSkip(
        source: JsonObject,
        objectName: String,
        startName: String,
        endName: String
    ): SkipInterval? {
        val nested = source.optJsonObject(objectName)
        val startSeconds = if (nested != null) {
            nested.optDouble("start", -1.0)
        } else {
            source.optDouble(startName, -1.0)
        }
        val endSeconds = if (nested != null) {
            nested.optDouble("end", -1.0)
        } else {
            source.optDouble(endName, -1.0)
        }
        if (startSeconds < 0 || endSeconds < 0) return null
        return SkipInterval(
            startMs = startSeconds.toMillis(),
            endMs = endSeconds.toMillis(),
            type = if (objectName == "intro") "op" else "ed"
        )
    }

    private fun JsonObject.optDouble(name: String, fallback: Double): Double {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive ?: return fallback
        return when {
            value.isNumber -> value.asDouble
            else -> value.asString.toDoubleOrNull() ?: fallback
        }
    }

    private fun JsonObject.optSeconds(primary: String, secondary: String): Double {
        return optDouble(primary, optDouble(secondary, -1.0))
    }

    private fun Double.toMillis(): Long = (this * 1000).toLong()

    private fun JsonObject.optString(name: String): String {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive ?: return ""
        return if (value.isNumber) value.asNumber.toString() else value.asString
    }

    private fun JsonObject.optJsonObject(name: String): JsonObject? {
        return get(name)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
    }

    private fun JsonObject.optJsonArray(name: String): JsonArray {
        return get(name)?.takeIf(JsonElement::isJsonArray)?.asJsonArray ?: JsonArray()
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return takeIf(JsonElement::isJsonObject)?.asJsonObject
    }

    private fun String.asJsonObjectOrNull(): JsonObject? {
        return runCatching { JsonParser.parseString(this) }.getOrNull()?.asJsonObjectOrNull()
    }

    private fun String.asJsonArrayOrNull(): JsonArray? {
        return runCatching { JsonParser.parseString(this) }.getOrNull()
            ?.takeIf(JsonElement::isJsonArray)?.asJsonArray
    }

    const val BACKEND_ANIDB = "anidb"
    const val BACKEND_ANINEKO = "anineko"
    private const val CAPTION_QUERY_KEY = "caption_1="
}
