package msr.atsulab.app.player.storage

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal class SourceMappingJsonCodec(
    private val gson: Gson
) {

    fun encode(mapping: SourceMapping): String {
        return gson.toJson(normalize(mapping))
    }

    fun decode(raw: String?, aniListId: String): SourceMapping? {
        if (raw.isNullOrBlank() || aniListId.isBlank()) return null

        return try {
            val root = JsonParser.parseString(raw).asJsonObject

            val picksJson = root.getAsJsonObject("picks")
            val picks = LinkedHashMap<String, SourcePick>()
            picksJson?.entrySet()?.forEach { (key, value) ->
                if (!value.isJsonObject || key.isBlank()) return@forEach
                val pickJson = value.asJsonObject
                picks[key] = SourcePick(
                    id = pickJson.stringOrEmpty("id"),
                    title = pickJson.stringOrEmpty("title"),
                    thumbnailUrl = pickJson.stringOrEmpty("thumbnailUrl")
                )
            }

            val skippedJson = root.getAsJsonArray("skipped")
            val skipped = skippedJson
                ?.mapIndexedNotNull { _, element ->
                    if (element.isJsonPrimitive && element.asJsonPrimitive.isString) element.asString else null
                }
                ?.filter { it.isNotBlank() }
                ?.toLinkedHashSet()
                .orEmpty()

            SourceMapping(
                aniListId = aniListId.trim(),
                picks = picks,
                skipped = skipped,
                confirmedAt = root.get("confirmedAt")
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asLong ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun normalize(mapping: SourceMapping): SourceMapping {
        return mapping.copy(
            aniListId = mapping.aniListId.trim(),
            picks = LinkedHashMap(
                mapping.picks.filterKeys { it.isNotBlank() }.mapValues { (_, pick) ->
                    pick.copy(
                        id = pick.id,
                        title = pick.title,
                        thumbnailUrl = pick.thumbnailUrl
                    )
                }
            ),
            skipped = LinkedHashSet(mapping.skipped.filter { it.isNotBlank() })
        )
    }
}

private fun JsonObject.stringOrEmpty(name: String): String {
    val value = get(name)
    return if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
        value.asString
    } else {
        ""
    }
}

private fun Collection<String>.toLinkedHashSet(): Set<String> {
    return LinkedHashSet(this)
}

internal fun SourceMapping.mergedWith(
    existing: SourceMapping?,
    confirmedAt: Long
): SourceMapping {
    return copy(
        aniListId = aniListId.trim(),
        picks = LinkedHashMap(existing?.picks.orEmpty() + picks),
        skipped = LinkedHashSet(existing?.skipped.orEmpty() + skipped),
        confirmedAt = confirmedAt
    )
}
