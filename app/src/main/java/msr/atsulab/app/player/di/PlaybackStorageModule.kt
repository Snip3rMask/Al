package msr.atsulab.app.player.di

import msr.atsulab.app.player.storage.DefaultSourceMappingStore
import msr.atsulab.app.player.storage.SourceMappingStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playbackStorageModule = module {
    single<PlaybackDiagnostics> {
        if (BuildConfig.DEBUG) AndroidLogPlaybackDiagnostics() else NoOpPlaybackDiagnostics
    }
    single<SourceMappingStore> {
        DefaultSourceMappingStore(androidContext(), diagnostics = get())
    }
}
import msr.atsulab.app.BuildConfig
import msr.atsulab.app.player.diagnostics.AndroidLogPlaybackDiagnostics
import msr.atsulab.app.player.diagnostics.NoOpPlaybackDiagnostics
import msr.atsulab.app.player.diagnostics.PlaybackDiagnostics
