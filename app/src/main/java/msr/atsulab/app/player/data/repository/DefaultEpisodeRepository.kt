package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.provider.SourceProvider
import msr.atsulab.app.player.domain.repository.EpisodeRepository

class DefaultEpisodeRepository(
    private val providers: List<SourceProvider>
) : EpisodeRepository {

    override fun getEpisodes(anime: PlaybackAnime): Single<List<PlaybackEpisode>> {
        return loadFromProvider(0, anime)
    }

    private fun loadFromProvider(index: Int, anime: PlaybackAnime): Single<List<PlaybackEpisode>> {
        if (index >= providers.size) return Single.just(emptyList())
        val provider = providers[index]

        return provider.findCandidates(anime.title, anime.aniListId)
            .flatMap { candidates ->
                val candidate = selectCandidate(provider.id, candidates)
                if (candidate == null) {
                    loadFromProvider(index + 1, anime)
                } else {
                    provider.getEpisodes(candidate)
                        .map { episodes -> normalizeEpisodes(provider.id, anime, episodes) }
                        .flatMap { episodes ->
                            if (episodes.isEmpty()) {
                                loadFromProvider(index + 1, anime)
                            } else {
                                Single.just(episodes)
                            }
                        }
                }
            }
            .onErrorResumeNext { loadFromProvider(index + 1, anime) }
    }

    private fun selectCandidate(
        providerId: String,
        candidates: List<SourceCandidate>
    ): SourceCandidate? {
        val validCandidates = candidates.filter { it.id.isNotBlank() }
        if (providerId == AnifuxSourceIds.ANIFUX) {
            return validCandidates.firstOrNull {
                it.backendProvider == AnifuxSourceIds.BACKEND_ANIDB
            } ?: validCandidates.firstOrNull()
        }
        return validCandidates.firstOrNull()
    }

    private fun normalizeEpisodes(
        providerId: String,
        anime: PlaybackAnime,
        episodes: List<PlaybackEpisode>
    ): List<PlaybackEpisode> {
        return episodes.map { episode ->
            episode.copy(
                thumbnailUrl = episode.thumbnailUrl.ifBlank { anime.coverImageUrl },
                postTitle = episode.postTitle.ifBlank { anime.title },
                aniListId = episode.aniListId ?: anime.aniListId,
                providerId = providerId
            )
        }
    }

    internal object AnifuxSourceIds {
        const val ANIFUX = "anifux"
        const val BACKEND_ANIDB = "anidb"
    }
}
