package msr.atsulab.app.player.di

import msr.atsulab.app.player.storage.DefaultSourceMappingStore
import msr.atsulab.app.player.storage.SourceMappingStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playbackStorageModule = module {
    single<SourceMappingStore> {
        DefaultSourceMappingStore(androidContext())
    }
}
