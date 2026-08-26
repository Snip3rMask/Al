package msr.atsulab.app.player.download

import java.io.File

data class CompletedDownload(
    val aniListId: Int,
    val episodeId: String,
    val title: String,
    val filePath: String,
    val quality: String,
    val sizeBytes: Long,
    val completedAtEpochMs: Long
) {
    val key: String
        get() = filePath

    val file: File?
        get() = filePath.takeIf { it.isNotBlank() }?.let(::File)

    companion object {
        fun from(request: DownloadRequest, file: File, completedAtEpochMs: Long): CompletedDownload {
            return CompletedDownload(
                aniListId = request.aniListId,
                episodeId = request.episodeId,
                title = request.displayName,
                filePath = file.absolutePath,
                quality = request.quality,
                sizeBytes = file.length(),
                completedAtEpochMs = completedAtEpochMs
            )
        }
    }
}

interface DownloadEntryStore {
    fun all(): List<CompletedDownload>
    fun save(entry: CompletedDownload)
    fun remove(key: String)
    fun clear()
}

class DefaultDownloadEntryStore(
    context: android.content.Context,
    private val gson: com.google.gson.Gson
) : DownloadEntryStore {

    private val preferences by lazy {
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
    }

    override fun all(): List<CompletedDownload> {
        val entries = preferences.getString(ENTRIES_KEY, null)
            ?.let { raw -> gson.decodeEntries(raw) }
            .orEmpty()
            .filter { it.filePath.isNotBlank() }
            .sortedByDescending(CompletedDownload::completedAtEpochMs)
        return entries
    }

    override fun save(entry: CompletedDownload) {
        if (entry.key.isBlank()) return
        val entries = all().filterNot { it.key == entry.key } + entry
        preferences.edit()
            .putString(ENTRIES_KEY, gson.encodeEntries(entries))
            .apply()
    }

    override fun remove(key: String) {
        if (key.isBlank()) return
        val entries = all().filterNot { it.key == key }
        preferences.edit().putString(ENTRIES_KEY, gson.encodeEntries(entries)).apply()
    }

    override fun clear() {
        preferences.edit().remove(ENTRIES_KEY).apply()
    }

    private fun com.google.gson.Gson.decodeEntries(raw: String): List<CompletedDownload> =
        try {
            fromJson(raw, Array<CompletedDownload>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }

    private fun com.google.gson.Gson.encodeEntries(entries: List<CompletedDownload>): String =
        toJson(entries)

    private companion object {
        const val PREFERENCES_NAME = "atsu_player_downloads"
        const val ENTRIES_KEY = "completed"
    }
}
