package msr.atsulab.app.player.di

import msr.atsulab.app.player.data.provider.DakiSourceProvider
import msr.atsulab.app.player.domain.provider.SourceProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class PlaybackProviderModuleTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `daki provider is available through its qualified contract`() {
        startKoin { modules(playbackNetworkModule, playbackProviderModule) }
        val koin = org.koin.core.context.GlobalContext.get()

        val provider: SourceProvider = koin.get(
            qualifier = PlaybackProviderQualifiers.dakiSourceProvider
        )

        assertEquals(DakiSourceProvider.PROVIDER_ID, provider.id)
        assertEquals(DakiSourceProvider.DISPLAY_NAME, provider.displayName)
    }
}
