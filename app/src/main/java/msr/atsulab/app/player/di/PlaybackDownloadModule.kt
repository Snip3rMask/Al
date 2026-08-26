package msr.atsulab.app.player.di

import com.google.gson.Gson
import msr.atsulab.app.player.download.DefaultDownloadEntryStore
import msr.atsulab.app.player.download.DefaultDownloadQueueStore
import msr.atsulab.app.player.download.DownloadEntryStore
import msr.atsulab.app.player.download.DownloadQueueStore
import msr.atsulab.app.player.download.DownloadStorageLocation
import msr.atsulab.app.player.download.HlsDownloader
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val playbackDownloadModule = module {
    single<DownloadQueueStore> { DefaultDownloadQueueStore(androidContext(), Gson()) }
    single<DownloadEntryStore> { DefaultDownloadEntryStore(androidContext(), Gson()) }
    single {
        val context = androidContext()
        HlsDownloader(
            httpClient = get(PlaybackNetworkQualifiers.playbackHttpClient),
            outputRootProvider = {
                val preferences = get<PlaybackPreferencesStore>()
                when (preferences.getDownloadStorageLocation()) {
                    DownloadStorageLocation.INTERNAL -> File(context.filesDir, DOWNLOAD_DIRECTORY)
                    DownloadStorageLocation.EXTERNAL_APP ->
                        File(context.getExternalFilesDir(null) ?: context.filesDir, DOWNLOAD_DIRECTORY)
                }
            },
            maxParallelSegments = HlsDownloader.MAX_PARALLEL_LIMIT
        )
    }
}

private const val DOWNLOAD_DIRECTORY = "player-downloads"
