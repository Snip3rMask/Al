package msr.atsulab.app.player.di

import com.google.gson.Gson
import msr.atsulab.app.player.download.DefaultDownloadEntryStore
import msr.atsulab.app.player.download.DownloadEntryStore
import msr.atsulab.app.player.download.DownloadQueueStore
import msr.atsulab.app.player.download.HlsDownloader
import msr.atsulab.app.player.download.InMemoryDownloadQueueStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val playbackDownloadModule = module {
    single { InMemoryDownloadQueueStore() }
    single<DownloadQueueStore> { get<InMemoryDownloadQueueStore>() }
    single<DownloadEntryStore> { DefaultDownloadEntryStore(androidContext(), Gson()) }
    single {
        HlsDownloader(
            httpClient = get(PlaybackNetworkQualifiers.playbackHttpClient),
            outputDirectory = File(androidContext().filesDir, DOWNLOAD_DIRECTORY)
        )
    }
}

private const val DOWNLOAD_DIRECTORY = "player-downloads"
