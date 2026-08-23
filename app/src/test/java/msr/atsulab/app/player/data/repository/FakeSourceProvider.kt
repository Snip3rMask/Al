package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.domain.provider.SourceProvider

class FakeSourceProvider(
    override val id: String,
    override val displayName: String = id,
    private val candidates: List<SourceCandidate> = listOf(SourceCandidate(id = "$id-id", title = "AtsuLab Anime")),
    private val episodes: List<PlaybackEpisode> = emptyList(),
    private val sources: List<VideoSource> = emptyList(),
    private val candidateError: Boolean = false,
    private val episodeError: Boolean = false,
    private val sourceError: Boolean = false
) : SourceProvider {

    var candidateCalls = 0
        private set
    var episodeCalls = 0
        private set
    var sourceCalls = 0
        private set

    override fun findCandidates(title: String, aniListId: Int?): Single<List<SourceCandidate>> {
        candidateCalls++
        return if (candidateError) Single.error(IllegalStateException("$id candidates failed"))
        else Single.just(candidates)
    }

    override fun getEpisodes(candidate: SourceCandidate): Single<List<PlaybackEpisode>> {
        episodeCalls++
        return if (episodeError) Single.error(IllegalStateException("$id episodes failed"))
        else Single.just(episodes)
    }

    override fun getSources(
        candidate: SourceCandidate,
        episode: PlaybackEpisode,
        preferredLanguage: String?
    ): Single<List<VideoSource>> {
        sourceCalls++
        requestedEpisodes += episode
        return if (sourceError) Single.error(IllegalStateException("$id sources failed"))
        else Single.just(sources)
    }

    val requestedEpisodes = mutableListOf<PlaybackEpisode>()
}
