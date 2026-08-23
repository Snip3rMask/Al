package msr.atsulab.app.player.storage

import android.content.Context
import com.google.gson.Gson

class DefaultSourceMappingStore(
    context: Context,
    gson: Gson = Gson(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : SourceMappingStore {

    private val codec = SourceMappingJsonCodec(gson)
    private val preferences by lazy {
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun get(aniListId: String): SourceMapping? {
        val key = aniListId.trim()
        if (key.isEmpty()) return null

        val raw = preferences.getString(key, null)
        val mapping = codec.decode(raw, key)
        if (mapping == null && raw != null) {
            preferences.edit().remove(key).apply()
        }
        return mapping
    }

    override fun save(mapping: SourceMapping) {
        val aniListId = mapping.aniListId.trim()
        if (aniListId.isEmpty()) return

        val existing = get(aniListId)
        val merged = mapping.mergedWith(existing, currentTimeMillis())
        preferences.edit().putString(aniListId, codec.encode(merged)).apply()
    }

    override fun has(aniListId: String): Boolean {
        val mapping = get(aniListId)
        return mapping != null && mapping.picks.isNotEmpty()
    }

    override fun clear(aniListId: String) {
        val key = aniListId.trim()
        if (key.isEmpty()) return
        preferences.edit().remove(key).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "atsu_source_mappings"
    }
}
