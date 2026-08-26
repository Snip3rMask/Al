package msr.atsulab.app.player.storage

import android.content.Context
import com.google.gson.Gson
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import msr.atsulab.app.player.domain.model.PlaybackProgress
import msr.atsulab.app.player.domain.repository.PlaybackProgressRepository

internal fun playbackProgressKey(progress: PlaybackProgress): String {
    return listOfNotNull(
        progress.aniListId?.toString(),
        progress.playbackId.trim(),
        progress.episodeUrl.trim()
    ).joinToString("|")
}

internal fun isStorablePlaybackProgress(progress: PlaybackProgress): Boolean {
    return progress.playbackId.isNotBlank() &&
        progress.episodeUrl.isNotBlank() &&
        progress.positionMs >= 0L &&
        progress.durationMs > 0L &&
        progress.positionMs <= progress.durationMs
}

class DefaultPlaybackProgressRepository(
    context: Context,
    private val gson: Gson = Gson()
) : PlaybackProgressRepository {

    private val preferences by lazy {
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    private val subject by lazy { BehaviorSubject.createDefault(readAll()) }

    override fun observeAll(): Observable<List<PlaybackProgress>> {
        return subject.map(List<PlaybackProgress>::sortedRecentFirst)
    }

    override fun upsert(progress: PlaybackProgress): Completable {
        return Completable.fromAction {
            if (!isStorablePlaybackProgress(progress)) return@fromAction

            synchronized(this) {
                val normalized = progress.copy(
                    playbackId = progress.playbackId.trim(),
                    episodeUrl = progress.episodeUrl.trim(),
                    animeTitle = progress.animeTitle.trim(),
                    episodeName = progress.episodeName.trim(),
                    sourceDisplayName = progress.sourceDisplayName.trim().ifBlank { progress.sourceId },
                    quality = progress.quality.trim(),
                    positionMs = progress.positionMs.coerceIn(0L, progress.durationMs),
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                val key = playbackProgressKey(normalized)
                val entries = readAllLocked().filterNot { playbackProgressKey(it) == key } + normalized
                writeAllLocked(entries.sortedRecentFirst().take(MAX_ENTRIES))
            }
        }
    }

    override fun remove(aniListId: Int?, playbackId: String, episodeUrl: String): Completable {
        return clearMatching(aniListId, playbackId, episodeUrl)
    }

    override fun clear(aniListId: Int?, playbackId: String): Completable {
        return clearMatching(aniListId, playbackId, episodeUrl = null)
    }

    override fun clearAll(): Completable {
        return Completable.fromAction {
            synchronized(this) {
                preferences.edit().remove(ENTRIES_KEY).apply()
            }
            refreshSubject()
        }
    }

    fun find(aniListId: Int?, playbackId: String, episodeUrl: String): PlaybackProgress? {
        return synchronized(this) {
            readAllLocked().firstOrNull { progress ->
                progress.aniListId == aniListId &&
                    progress.playbackId == playbackId.trim() &&
                    progress.episodeUrl == episodeUrl.trim()
            }
        }
    }

    private fun clearMatching(
        aniListId: Int?,
        playbackId: String,
        episodeUrl: String?
    ): Completable {
        return Completable.fromAction {
            val trimmedPlaybackId = playbackId.trim()
            val trimmedEpisodeUrl = episodeUrl?.trim()
            synchronized(this) {
                val remaining = readAllLocked().filterNot { progress ->
                    progress.aniListId == aniListId &&
                        progress.playbackId == trimmedPlaybackId &&
                        (trimmedEpisodeUrl == null || progress.episodeUrl == trimmedEpisodeUrl)
                }
                writeAllLocked(remaining)
            }
            refreshSubject()
        }
    }

    private fun refreshSubject() {
        subject.onNext(readAll())
    }

    private fun readAll(): List<PlaybackProgress> {
        return synchronized(this) { readAllLocked() }
    }

    private fun readAllLocked(): List<PlaybackProgress> {
        val raw = preferences.getString(ENTRIES_KEY, null) ?: return emptyList()
        return try {
            gson.fromJson(raw, Array<PlaybackProgress>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeAllLocked(entries: List<PlaybackProgress>) {
        preferences.edit().putString(ENTRIES_KEY, gson.toJson(entries)).apply()
        subject.onNext(entries.sortedRecentFirst())
    }

    private companion object {
        const val PREFERENCES_NAME = "atsu_playback_progress"
        const val ENTRIES_KEY = "entries"
        const val MAX_ENTRIES = 100
    }
}

private fun List<PlaybackProgress>.sortedRecentFirst(): List<PlaybackProgress> {
    return sortedByDescending(PlaybackProgress::updatedAtEpochMs)
}
