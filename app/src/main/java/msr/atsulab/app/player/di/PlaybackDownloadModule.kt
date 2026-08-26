package msr.atsulab.app.player.di

import msr.atsulab.app.player.download.HlsDownloader
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val playbackDownloadModule = module {
    single {
        HlsDownloader(
            httpClient = get(PlaybackNetworkQualifiers.playbackHttpClient),
            outputDirectory = File(androidContext().filesDir, DOWNLOAD_DIRECTORY)
        )
    }
}

private const val DOWNLOAD_DIRECTORY = "player-downloads"
