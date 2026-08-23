package msr.atsulab.app.player.domain.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SkipInterval

interface SkipTimeRepository {
    fun getSkipIntervals(
        anime: PlaybackAnime,
        episode: PlaybackEpisode,
        durationMs: Long = 0L
    ): Single<List<SkipInterval>>
}
