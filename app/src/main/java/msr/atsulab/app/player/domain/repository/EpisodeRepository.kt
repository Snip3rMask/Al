package msr.atsulab.app.player.domain.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode

interface EpisodeRepository {
    fun getEpisodes(anime: PlaybackAnime): Single<List<PlaybackEpisode>>
}
