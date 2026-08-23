package msr.atsulab.app.player.di

import msr.atsulab.app.player.data.provider.DakiSourceProvider
import msr.atsulab.app.player.data.provider.MkissaSourceProvider
import msr.atsulab.app.player.domain.provider.SourceProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

object PlaybackProviderQualifiers {
    val dakiSourceProvider = named(DakiSourceProvider.PROVIDER_ID)
    val mkissaSourceProvider = named(MkissaSourceProvider.PROVIDER_ID)
}

val playbackProviderModule = module {
    single<SourceProvider>(qualifier = PlaybackProviderQualifiers.dakiSourceProvider) {
        DakiSourceProvider(
            client = get(qualifier = PlaybackNetworkQualifiers.playbackHttpClient)
        )
    }
    single<SourceProvider>(qualifier = PlaybackProviderQualifiers.mkissaSourceProvider) {
        MkissaSourceProvider(
            client = get(qualifier = PlaybackNetworkQualifiers.playbackHttpClient)
        )
    }
}
