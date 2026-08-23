package msr.atsulab.app.player.domain.provider

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource

interface SourceProvider {
    val id: String
    val displayName: String

    fun findCandidates(
        title: String,
        aniListId: Int?
    ): Single<List<SourceCandidate>>

    fun getEpisodes(candidate: SourceCandidate): Single<List<PlaybackEpisode>>

    fun getSources(
        candidate: SourceCandidate,
        episode: PlaybackEpisode,
        preferredLanguage: String? = null
    ): Single<List<VideoSource>>
}
