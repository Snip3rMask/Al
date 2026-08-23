package msr.atsulab.app.player.di

import okhttp3.OkHttpClient
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

object PlaybackNetworkQualifiers {
    val playbackHttpClient: Qualifier = named("playbackHttpClient")
}

object PlaybackHttpClientFactory {

    fun create(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 35L
}

val playbackNetworkModule = module {
    single(qualifier = PlaybackNetworkQualifiers.playbackHttpClient) {
        PlaybackHttpClientFactory.create()
    }
}
