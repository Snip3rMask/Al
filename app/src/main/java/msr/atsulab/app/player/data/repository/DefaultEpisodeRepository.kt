package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.diagnostics.NoOpPlaybackDiagnostics
import msr.atsulab.app.player.diagnostics.PlaybackDiagnostics
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.provider.SourceProvider
import msr.atsulab.app.player.domain.repository.EpisodeRepository
import msr.atsulab.app.player.storage.SourceMapping
import msr.atsulab.app.player.storage.SourceMappingStore

class DefaultEpisodeRepository(
    private val providers: List<SourceProvider>,
    private val diagnostics: PlaybackDiagnostics = NoOpPlaybackDiagnostics,
    private val sourceMappingStore: SourceMappingStore? = null
) : EpisodeRepository {

    override fun getEpisodes(anime: PlaybackAnime): Single<List<PlaybackEpisode>> {
        val mapping = anime.aniListId?.toString()
            ?.takeUnless(String::isBlank)
            ?.let { aniListId -> sourceMappingStore?.get(aniListId) }
        return loadFromProvider(0, anime, mapping)
    }

    private fun loadFromProvider(
        index: Int,
        anime: PlaybackAnime,
        mapping: SourceMapping?
    ): Single<List<PlaybackEpisode>> {
        if (index >= providers.size) return Single.just(emptyList())
        val provider = providers[index]

        return provider.findCandidateGroups(anime.title, anime.aniListId)
            .flatMap { groups ->
                val candidate = selectCandidate(provider.id, groups, mapping)
                if (candidate == null) {
                    diagnostics.onEpisodeProviderSkipped(
                        providerId = provider.id,
                        providerIndex = index,
                        aniListId = anime.aniListId,
                        reason = "no-compatible-candidate"
                    )
                    loadFromProvider(index + 1, anime, mapping)
                } else {
                    provider.getEpisodes(candidate)
                        .map { episodes -> normalizeEpisodes(provider.id, anime, episodes) }
                        .flatMap { episodes ->
                            if (episodes.isEmpty()) {
                                diagnostics.onEpisodeProviderSkipped(
                                    providerId = provider.id,
                                    providerIndex = index,
                                    aniListId = anime.aniListId,
                                    reason = "no-episodes"
                                )
                                loadFromProvider(index + 1, anime, mapping)
                            } else {
                                Single.just(episodes)
                            }
                        }
                }
            }
            .onErrorResumeNext { error ->
                diagnostics.onEpisodeProviderFailed(provider.id, index, anime.aniListId, error)
                loadFromProvider(index + 1, anime, mapping)
            }
    }

    private fun selectCandidate(
        providerId: String,
        groups: Map<String, List<SourceCandidate>>,
        mapping: SourceMapping?
    ): SourceCandidate? {
        if (!providerHasConfirmedConstraint(providerId, mapping)) return null

        val primaryLabel = if (providerId == AnifuxSourceIds.ANIFUX) {
            AnifuxSourceIds.DAKI_DISPLAY_NAME
        } else {
            providers.firstOrNull { it.id == providerId }?.displayName ?: providerId
        }
        if (mapping?.skipped?.contains(primaryLabel) == true) return null

        val unskippedGroups = groups.filterKeys { label -> mapping?.skipped?.contains(label) != true }
        val availableCandidates = unskippedGroups.values.flatten().filter { it.id.isNotBlank() }
        if (availableCandidates.isEmpty()) return null

        val primaryPick = mapping?.picks?.get(primaryLabel)?.takeIf { it.id.isNotBlank() }
        if (primaryPick != null) {
            val confirmed = unskippedGroups[primaryLabel]
                .orEmpty()
                .firstOrNull { it.id == primaryPick.id }
                ?: return null
            return confirmed.withAuxiliarySource(providerId, mapping)
        }

        val defaultCandidate = when (providerId) {
            AnifuxSourceIds.ANIFUX -> availableCandidates.firstOrNull {
                it.backendProvider == AnifuxSourceIds.BACKEND_ANIDB
            } ?: availableCandidates.firstOrNull()
            else -> availableCandidates.firstOrNull()
        }
        return defaultCandidate?.withAuxiliarySource(providerId, mapping)
    }

    private fun providerHasConfirmedConstraint(
        providerId: String,
        mapping: SourceMapping?
    ): Boolean {
        if (mapping == null) return true

        val constrainedLabels = mapping.picks.keys + mapping.skipped
        if (constrainedLabels.isEmpty()) return true

        if (primaryLabel(providerId) in constrainedLabels) return true
        return providerId == AnifuxSourceIds.ANIFUX &&
            AnifuxSourceIds.NORA_DISPLAY_NAME in constrainedLabels
    }

    private fun primaryLabel(providerId: String): String {
        return if (providerId == AnifuxSourceIds.ANIFUX) {
            AnifuxSourceIds.DAKI_DISPLAY_NAME
        } else {
            providers.firstOrNull { it.id == providerId }?.displayName ?: providerId
        }
    }

    private fun SourceCandidate.withAuxiliarySource(
        providerId: String,
        mapping: SourceMapping?
    ): SourceCandidate {
        if (providerId != AnifuxSourceIds.ANIFUX) return this
        if (backendProvider.isNotEmpty() && backendProvider != AnifuxSourceIds.BACKEND_ANIDB) return this
        val slug = mapping?.picks?.get(AnifuxSourceIds.NORA_DISPLAY_NAME)?.id.orEmpty()
        return copy(confirmedSourceSlug = slug)
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
        const val DAKI_DISPLAY_NAME = "Daki"
        const val NORA_DISPLAY_NAME = "Nora"
    }
}
