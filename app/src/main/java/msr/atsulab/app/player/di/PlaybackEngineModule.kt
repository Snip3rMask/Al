package msr.atsulab.app.player.di

import msr.atsulab.app.player.engine.DefaultPlaybackEngine
import msr.atsulab.app.player.engine.Media3EngineMediaPlayer
import msr.atsulab.app.player.engine.PlaybackEngine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playbackEngineModule = module {
    factory<PlaybackEngine> {
        DefaultPlaybackEngine(Media3EngineMediaPlayer(androidContext()))
    }
}
