package msr.atsulab.app.player.data.repository

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import msr.atsulab.app.player.diagnostics.NoOpPlaybackDiagnostics
import msr.atsulab.app.player.diagnostics.PlaybackDiagnostics
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.SourceCandidate
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.domain.provider.SourceProvider
import msr.atsulab.app.player.domain.repository.VideoSourceRepository

class DefaultVideoSourceRepository(
    private val providers: List<SourceProvider>,
    private val diagnostics: PlaybackDiagnostics = NoOpPlaybackDiagnostics
) : VideoSourceRepository {

    override fun getSources(
        anime: PlaybackAnime,
        episode: PlaybackEpisode,
        preferredLanguage: String?,
        preferredServer: String?
    ): Single<List<VideoSource>> {
        return resolveFromProviders(orderedProviders(episode.providerId), anime, episode)
            .map { sources -> sortByPreferences(sources, preferredLanguage, preferredServer) }
    }

    override fun getMoreSources(
        anime: PlaybackAnime,
        episode: PlaybackEpisode
    ): Single<List<VideoSource>> {
        val remainingProviders = orderedProviders(episode.providerId)
            .filter { it.id != episode.providerId }
        if (remainingProviders.isEmpty()) return Single.just(emptyList())

        return Observable.fromIterable(remainingProviders)
            .flatMapSingle { provider -> resolveProviderSources(provider, anime, episode) }
            .toList()
            .map { providerSources ->
                sortByPreferences(
                    providerSources.flatten(),
                    preferredLanguage = null,
                    preferredServer = null
                )
            }
    }

    private fun orderedProviders(providerId: String?): List<SourceProvider> {
        if (providerId.isNullOrBlank()) return providers
        val target = providers.firstOrNull { it.id == providerId } ?: return providers
        return listOf(target) + providers.filter { it.id != providerId }
    }

    private fun resolveFromProviders(
        providers: List<SourceProvider>,
        anime: PlaybackAnime,
        episode: PlaybackEpisode,
        index: Int = 0
    ): Single<List<VideoSource>> {
        if (index >= providers.size) return Single.just(emptyList())

        return resolveProviderSources(providers[index], anime, episode, index)
            .flatMap { sources ->
                if (sources.isEmpty()) resolveFromProviders(providers, anime, episode, index + 1)
                else Single.just(sources)
            }
    }

    private fun resolveProviderSources(
        provider: SourceProvider,
        anime: PlaybackAnime,
        episode: PlaybackEpisode,
        providerIndex: Int = 0
    ): Single<List<VideoSource>> {
        val candidate = SourceCandidate(
            id = episode.playbackId.ifBlank { anime.aniListId.toString() },
            title = episode.postTitle.ifBlank { anime.title },
            thumbnailUrl = episode.thumbnailUrl.ifBlank { anime.coverImageUrl }
        )

        return provider.getSources(candidate, episode)
            .map { sources -> sources.map { it.copy(providerId = provider.id) } }
            .flatMap { sources ->
                if (sources.isEmpty()) {
                    diagnostics.onSourceProviderSkipped(
                        providerId = provider.id,
                        providerIndex = providerIndex,
                        playbackId = episode.playbackId.ifBlank { anime.aniListId.toString() },
                        reason = "no-sources"
                    )
                }
                Single.just(sources)
            }
            .onErrorResumeNext { error ->
                diagnostics.onSourceProviderFailed(
                    providerId = provider.id,
                    providerIndex = providerIndex,
                    playbackId = episode.playbackId.ifBlank { anime.aniListId.toString() },
                    error = error
                )
                Single.just(emptyList())
            }
    }

    private fun sortByPreferences(
        sources: List<VideoSource>,
        preferredLanguage: String?,
        preferredServer: String?
    ): List<VideoSource> {
        return sources.sortedWith(
            compareByDescending<VideoSource> { source -> serverScore(source, preferredServer) }.thenByDescending { source -> languageScore(source, preferredLanguage) }
        )
    }

    private fun serverScore(source: VideoSource, preferredServer: String?): Int {
        val value = preferredServer?.trim().orEmpty()
        if (value.isEmpty()) return 0
        return when {
            source.displayName.equals(value, ignoreCase = true) ||
                source.server.equals(value, ignoreCase = true) -> 2
            source.displayName.contains(value, ignoreCase = true) ||
                source.server.contains(value, ignoreCase = true) -> 1
            else -> 0
        }
    }

    private fun languageScore(source: VideoSource, preferredLanguage: String?): Int {
        val value = preferredLanguage?.trim().orEmpty()
        if (value.isEmpty()) return 0
        return when {
            value.equals(PREFERRED_DUB, ignoreCase = true) -> if (isDub(source)) 1 else 0
            value.equals(PREFERRED_SUB, ignoreCase = true) -> if (!isDub(source)) 1 else 0
            else -> listOf(source.language, source.server, source.quality).any {
                it.contains(value, ignoreCase = true)
            }.compareTo(false)
        }
    }

    private fun isDub(source: VideoSource): Boolean {
        return listOf(source.language, source.server, source.quality).any {
            it.contains(DUB_KEYWORD, ignoreCase = true)
        }
    }

    private companion object {
        const val PREFERRED_SUB = "sub"
        const val PREFERRED_DUB = "dub"
        const val DUB_KEYWORD = "dub"
    }
}
