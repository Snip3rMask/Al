package msr.atsulab.app.player.di

import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class PlaybackNetworkModuleTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `playback http client is a qualified singleton`() {
        startKoin { modules(playbackNetworkModule) }
        val koin = GlobalContext.get()

        assertEquals(
            koin.get(qualifier = PlaybackNetworkQualifiers.playbackHttpClient),
            koin.get(qualifier = PlaybackNetworkQualifiers.playbackHttpClient)
        )
    }
}
