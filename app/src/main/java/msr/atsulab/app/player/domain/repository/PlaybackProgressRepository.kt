package msr.atsulab.app.player.domain.repository

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import msr.atsulab.app.player.domain.model.PlaybackProgress

interface PlaybackProgressRepository {
    fun observeAll(): Observable<List<PlaybackProgress>>

    fun upsert(progress: PlaybackProgress): Completable

    fun remove(
        aniListId: Int?,
        playbackId: String,
        episodeUrl: String
    ): Completable

    fun clear(aniListId: Int?, playbackId: String): Completable

    fun clearAll(): Completable
}
