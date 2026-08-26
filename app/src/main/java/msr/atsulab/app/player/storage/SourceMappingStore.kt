package msr.atsulab.app.player.storage

data class SourcePick(
    val id: String = "",
    val title: String = "",
    val thumbnailUrl: String = ""
)

data class SourceMapping(
    val aniListId: String = "",
    val picks: Map<String, SourcePick> = emptyMap(),
    val skipped: Set<String> = emptySet(),
    val confirmedAt: Long = 0L
)

interface SourceMappingStore {
    fun get(aniListId: String): SourceMapping?

    fun save(mapping: SourceMapping)

    fun has(aniListId: String): Boolean

    fun clear(aniListId: String)

    fun replace(mapping: SourceMapping) {
        val key = mapping.aniListId.trim()
        if (key.isNotEmpty()) clear(key)
        save(mapping)
    }
}
