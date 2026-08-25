package msr.atsulab.app.ui.settings.capture

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import msr.atsulab.app.ui.base.BaseViewModel

class CaptureSettingsViewModel(
    private val playbackPreferencesStore: PlaybackPreferencesStore
) : BaseViewModel<Unit>() {

    private val _isFrameCaptureEnabled = BehaviorSubject.createDefault(
        playbackPreferencesStore.isFrameCaptureEnabled()
    )
    val isFrameCaptureEnabled: Observable<Boolean>
        get() = _isFrameCaptureEnabled

    private val _isFrameCaptureAlwaysVisible = BehaviorSubject.createDefault(
        playbackPreferencesStore.isFrameCaptureAlwaysVisible()
    )
    val isFrameCaptureAlwaysVisible: Observable<Boolean>
        get() = _isFrameCaptureAlwaysVisible

    private val _frameCaptureDirectoryUri = BehaviorSubject.createDefault(
        playbackPreferencesStore.getFrameCaptureDirectoryUri()
    )
    val frameCaptureDirectoryUri: Observable<String>
        get() = _frameCaptureDirectoryUri

    fun setEnabled(enabled: Boolean) {
        playbackPreferencesStore.setFrameCaptureEnabled(enabled)
        _isFrameCaptureEnabled.onNext(enabled)
    }

    fun setAlwaysVisible(enabled: Boolean) {
        playbackPreferencesStore.setFrameCaptureAlwaysVisible(enabled)
        _isFrameCaptureAlwaysVisible.onNext(enabled)
    }

    fun setDirectory(uri: String) {
        playbackPreferencesStore.setFrameCaptureDirectoryUri(uri)
        _frameCaptureDirectoryUri.onNext(uri)
    }

    fun resetButtonPosition() {
        playbackPreferencesStore.clearFrameCapturePosition()
    }

    override fun loadData(param: Unit) = Unit
}
