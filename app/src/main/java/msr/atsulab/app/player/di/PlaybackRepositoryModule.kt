package msr.atsulab.app.player.di

import msr.atsulab.app.player.data.repository.DefaultEpisodeRepository
import msr.atsulab.app.player.data.repository.DefaultSkipTimeRepository
import msr.atsulab.app.player.data.repository.DefaultVideoSourceRepository
import msr.atsulab.app.player.domain.provider.SourceProvider
import msr.atsulab.app.player.domain.repository.EpisodeRepository
import msr.atsulab.app.player.domain.repository.SkipTimeRepository
import msr.atsulab.app.player.domain.repository.VideoSourceRepository
import org.koin.dsl.module

val playbackRepositoryModule = module {
    single<EpisodeRepository> {
        DefaultEpisodeRepository(
            providers = listOf(
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.mkissaSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.anifuxSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.dakiSourceProvider)
            )
        )
    }
    single<VideoSourceRepository> {
        DefaultVideoSourceRepository(
            providers = listOf(
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.mkissaSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.anifuxSourceProvider),
                get<SourceProvider>(qualifier = PlaybackProviderQualifiers.dakiSourceProvider)
            )
        )
    }
    single<SkipTimeRepository> {
        DefaultSkipTimeRepository(
            client = get(qualifier = PlaybackNetworkQualifiers.playbackHttpClient)
        )
    }
}
