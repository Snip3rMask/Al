package msr.atsulab.app.player.data.provider

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate

internal object MkissaResponseParser {

    fun parseEpisodes(
        request: SourceCandidate,
        json: String
    ): List<PlaybackEpisode> {
        val detail = parseShow(json)?.optJsonObject("availableEpisodesDetail") ?: return emptyList()
        val subEpisodes = detail.optJsonArray("sub")
        val dubEpisodes = detail.optJsonArray("dub")
        val selectedEpisodes = if (subEpisodes.size() > 0) subEpisodes else dubEpisodes
        if (selectedEpisodes.size() == 0) return emptyList()

        val showId = parseShow(json)?.optString("_id")?.takeIf(String::isNotEmpty)
            ?: request.id.trim()

        return selectedEpisodes.asSequence()
            .mapNotNull(::optionalEpisodeValue)
            .filter(String::isNotEmpty)
            .mapNotNull { episodeValue ->
                val episodeNumber = parseEpisodeNumber(episodeValue)
                episodeNumber?.let { episodeValue to it }
            }
            .map { (episodeValue, episodeNumber) ->
                PlaybackEpisode(
                    name = "Episode $episodeValue",
                    url = "$showId/ep-$episodeValue",
                    thumbnailUrl = request.thumbnailUrl,
                    number = episodeNumber,
                    postTitle = request.title,
                    playbackId = showId,
                    confirmedSourceSlug = MkissaSourceProvider.PROVIDER_ID,
                    aniListId = request.id.trim().toIntOrNull()
                )
            }
            .sortedBy(PlaybackEpisode::number)
            .toList()
    }

    private fun parseShow(json: String): JsonObject? {
        return JsonParser.parseString(json).asJsonObject
            .optJsonObject("data")
            ?.optJsonObject("show")
    }

    private fun optionalEpisodeValue(element: JsonElement?): String? {
        return element?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive?.asString
    }

    private fun parseEpisodeNumber(value: String): Float? {
        return runCatching { value.toFloat() }.getOrNull()
    }

    private fun JsonObject.optString(name: String): String {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive) ?: return ""
        return value.asJsonPrimitive.asString
    }

    private fun JsonObject.optJsonObject(name: String): JsonObject? {
        return get(name)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
    }

    private fun JsonObject.optJsonArray(name: String): JsonArray {
        return get(name)?.takeIf(JsonElement::isJsonArray)?.asJsonArray ?: JsonArray()
    }
}
