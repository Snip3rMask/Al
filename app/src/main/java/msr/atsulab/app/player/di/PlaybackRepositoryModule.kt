package msr.atsulab.app.player.di

import msr.atsulab.app.player.data.repository.DefaultSkipTimeRepository
import msr.atsulab.app.player.domain.repository.SkipTimeRepository
import org.koin.dsl.module

val playbackRepositoryModule = module {
    single<SkipTimeRepository> {
        DefaultSkipTimeRepository(
            client = get(qualifier = PlaybackNetworkQualifiers.playbackHttpClient)
        )
    }
}
