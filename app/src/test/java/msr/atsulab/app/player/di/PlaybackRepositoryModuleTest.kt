package msr.atsulab.app.player.di

import msr.atsulab.app.player.data.repository.DefaultEpisodeRepository
import msr.atsulab.app.player.data.repository.DefaultSkipTimeRepository
import msr.atsulab.app.player.data.repository.DefaultVideoSourceRepository
import msr.atsulab.app.player.domain.repository.EpisodeRepository
import msr.atsulab.app.player.domain.repository.SkipTimeRepository
import msr.atsulab.app.player.domain.repository.VideoSourceRepository
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
        startKoin {
            modules(
                playbackNetworkModule,
                playbackProviderModule,
                playbackStorageModule,
                playbackRepositoryModule
            )
        }

        val repository: SkipTimeRepository = org.koin.core.context.GlobalContext.get().get()

        assertEquals(DefaultSkipTimeRepository::class, repository::class)
    }

    @Test
    fun `episode and video repositories resolve through domain contracts`() {
        startKoin {
            modules(
                playbackNetworkModule,
                playbackProviderModule,
                playbackStorageModule,
                playbackRepositoryModule
            )
        }
        val koin = org.koin.core.context.GlobalContext.get()

        assertEquals(DefaultEpisodeRepository::class, koin.get<EpisodeRepository>()::class)
        assertEquals(DefaultVideoSourceRepository::class, koin.get<VideoSourceRepository>()::class)
    }
}
