package msr.atsulab.app.ui.settings.player

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import msr.atsulab.app.ui.base.BaseViewModel
import org.koin.java.KoinJavaComponent.inject

class PlayerSettingsViewModel : BaseViewModel<Unit>() {

    private val preferencesStore: PlaybackPreferencesStore by inject(PlaybackPreferencesStore::class.java)

    private val _isAutoRotate = BehaviorSubject.createDefault(preferencesStore.isAutoRotateEnabled())
    val isAutoRotate: Observable<Boolean>
        get() = _isAutoRotate

    private val _isRememberLastEpisode = BehaviorSubject.createDefault(preferencesStore.isRememberLastEpisodeEnabled())
    val isRememberLastEpisode: Observable<Boolean>
        get() = _isRememberLastEpisode

    private val _seekDurationSeconds = BehaviorSubject.createDefault(preferencesStore.getSeekDurationMs() / 1000)
    val seekDurationSeconds: Observable<Int>
        get() = _seekDurationSeconds

    fun setAutoRotate(enabled: Boolean) {
        preferencesStore.setAutoRotateEnabled(enabled)
        _isAutoRotate.onNext(enabled)
    }

    fun setRememberLastEpisode(enabled: Boolean) {
        preferencesStore.setRememberLastEpisodeEnabled(enabled)
        _isRememberLastEpisode.onNext(enabled)
    }

    fun setSeekDuration(seconds: Int) {
        val clamped = seconds.coerceIn(5, 30)
        preferencesStore.setSeekDurationMs(clamped * 1000)
        _seekDurationSeconds.onNext(clamped)
    }
}
