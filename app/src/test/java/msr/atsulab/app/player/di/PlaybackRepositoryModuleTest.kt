package msr.atsulab.app.player.di

import msr.atsulab.app.player.data.repository.DefaultSkipTimeRepository
import msr.atsulab.app.player.domain.repository.SkipTimeRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class PlaybackRepositoryModuleTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `skip time repository resolves as its domain contract`() {
        startKoin { modules(playbackNetworkModule, playbackRepositoryModule) }

        val repository: SkipTimeRepository = org.koin.core.context.GlobalContext.get().get()

        assertEquals(DefaultSkipTimeRepository::class, repository::class)
    }
}
