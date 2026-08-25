package msr.atsulab.app.player.domain.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.VideoSource

interface VideoSourceRepository {
    fun getSources(
        anime: PlaybackAnime,
        episode: PlaybackEpisode,
        preferredLanguage: String? = null,
        preferredServer: String? = null
    ): Single<List<VideoSource>>

    fun getMoreSources(
        anime: PlaybackAnime,
        episode: PlaybackEpisode
    ): Single<List<VideoSource>> = Single.just(emptyList())
}
