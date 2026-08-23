package msr.atsulab.app.player.di

import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

class PlaybackNetworkModuleTest : KoinTest {

    private val firstClient: OkHttpClient by inject(
        qualifier = PlaybackNetworkQualifiers.playbackHttpClient
    )
    private val secondClient: OkHttpClient by inject(
        qualifier = PlaybackNetworkQualifiers.playbackHttpClient
    )

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `playback http client is a qualified singleton`() {
        startKoin { modules(playbackNetworkModule) }

        assertEquals(firstClient, secondClient)
    }
}
