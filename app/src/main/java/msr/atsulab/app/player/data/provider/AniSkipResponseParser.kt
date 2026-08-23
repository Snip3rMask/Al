package msr.atsulab.app.player.data.provider

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import msr.atsulab.app.player.domain.model.SkipInterval

internal object AniSkipResponseParser {

    fun resolveMalId(json: String): Int {
        val media = runCatching { JsonParser.parseString(json) }.getOrNull()
            ?.takeIf(JsonElement::isJsonObject)
            ?.asJsonObject
            ?.optJsonObject("data")
            ?.optJsonObject("Media")
            ?: return 0
        return media.optInt("idMal")
    }

    fun parseIntervals(json: String): List<SkipInterval> {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull()
            ?.takeIf(JsonElement::isJsonObject)?.asJsonObject
            ?: return emptyList()
        if (!root.optBoolean("found")) return emptyList()

        return root.optJsonArray("results").mapNotNull { element ->
            val item = element.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@mapNotNull null
            val interval = item.optJsonObject("interval") ?: return@mapNotNull null
            val startSeconds = interval.optDouble("startTime", -1.0)
            val endSeconds = interval.optDouble("endTime", -1.0)
            if (startSeconds < 0 || endSeconds <= startSeconds) return@mapNotNull null
            SkipInterval(
                startMs = Math.round(startSeconds * 1000),
                endMs = Math.round(endSeconds * 1000),
                type = item.optString("skipType").ifEmpty { SkipInterval.DEFAULT_TYPE }
            )
        }
    }

    private fun JsonObject.optInt(name: String): Int {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive ?: return 0
        return value.asInt
    }

    private fun JsonObject.optBoolean(name: String): Boolean {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive ?: return false
        return value.asBoolean
    }

    private fun JsonObject.optDouble(name: String, fallback: Double): Double {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive ?: return fallback
        return when {
            value.isNumber -> value.asDouble
            else -> value.asString.toDoubleOrNull() ?: fallback
        }
    }

    private fun JsonObject.optString(name: String): String {
        val value = get(name)?.takeIf(JsonElement::isJsonPrimitive)?.asJsonPrimitive ?: return ""
        return if (value.isNumber) value.asNumber.toString() else value.asString
    }

    private fun JsonObject.optJsonObject(name: String): JsonObject? {
        return get(name)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
    }

    private fun JsonObject.optJsonArray(name: String): List<JsonElement> {
        return get(name)?.takeIf(JsonElement::isJsonArray)?.asJsonArray.orEmpty().toList()
    }
}
