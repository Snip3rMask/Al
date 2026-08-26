package msr.atsulab.app.player.di

import msr.atsulab.app.player.data.repository.DefaultEpisodeRepository
import msr.atsulab.app.player.data.repository.DefaultSkipTimeRepository
import msr.atsulab.app.player.data.repository.DefaultSourceCandidateRepository
import msr.atsulab.app.player.data.repository.DefaultVideoSourceRepository
import msr.atsulab.app.player.domain.provider.SourceProvider
import msr.atsulab.app.player.domain.repository.EpisodeRepository
import msr.atsulab.app.player.domain.repository.SkipTimeRepository
import msr.atsulab.app.player.domain.repository.SourceCandidateRepository
import msr.atsulab.app.player.domain.repository.VideoSourceRepository
import msr.atsulab.app.player.storage.SourceMappingStore
import org.koin.dsl.module

val playbackRepositoryModule = module {
    single<EpisodeRepository> {
        DefaultEpisodeRepository(
            providers = listOf(
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.mkissaSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.anifuxSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.dakiSourceProvider)
            ),
            diagnostics = get(),
            sourceMappingStoreProvider = { get<SourceMappingStore>() }
        )
    }
    single<SourceCandidateRepository> {
        DefaultSourceCandidateRepository(
            providers = listOf(
                get(qualifier = PlaybackProviderQualifiers.dakiSourceProvider),
                get(qualifier = PlaybackProviderQualifiers.mkissaSourceProvider),
                get(qualifier = PlaybackProviderQualifiers.anifuxSourceProvider)
            )
        )
    }
    single<VideoSourceRepository> {
        DefaultVideoSourceRepository(
            providers = listOf(
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.mkissaSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.anifuxSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.dakiSourceProvider)
            ),
            diagnostics = get()
        )
    }
    single<SkipTimeRepository> {
        DefaultSkipTimeRepository(
            client = get(qualifier = PlaybackNetworkQualifiers.playbackHttpClient)
        )
    }
}
