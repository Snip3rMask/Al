package msr.atsulab.app.player.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import msr.atsulab.app.R
import msr.atsulab.app.player.domain.PlaybackSpeedOptions
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.domain.repository.EpisodeRepository
import msr.atsulab.app.player.domain.repository.VideoSourceRepository
import msr.atsulab.app.player.engine.PlaybackEngine
import msr.atsulab.app.player.engine.PlaybackEngineListener
import msr.atsulab.app.player.engine.PlaybackReadyState
import msr.atsulab.app.player.engine.PlaybackError
import msr.atsulab.app.player.engine.PlaybackState
import msr.atsulab.app.player.runtime.PlaybackEpisodeNavigator
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import org.koin.java.KoinJavaComponent.inject

class PlayerActivity : AppCompatActivity() {

    private val episodeRepository: EpisodeRepository by inject(EpisodeRepository::class.java)
    private val videoSourceRepository: VideoSourceRepository by inject(VideoSourceRepository::class.java)
    private val playbackEngine: PlaybackEngine by inject(PlaybackEngine::class.java)
    private val playbackPreferencesStore: PlaybackPreferencesStore by inject(PlaybackPreferencesStore::class.java)

    private lateinit var shell: PlayerShellViews
    private lateinit var playerView: PlayerView
    private lateinit var loadingIndicator: ProgressBar

    private val episodeNavigator = PlaybackEpisodeNavigator()

    private var currentAnime: PlaybackAnime? = null
    private var requestedEpisode = DEFAULT_INITIAL_EPISODE
    private var engineAttached = false
    private var playbackDisposable: Disposable? = null
    private var statusMessage = ""
    private var statusRetryVisible = false
    private var waitingForPlayback = true
    private var latestPlaybackState = PlaybackState()
    private var transportVisible = false
    private var controlsVisible = false
    private var controlsLocked = false
    private var gestureHandler: PlayerGestureHandler? = null
    private var playbackBrightness = BRIGHTNESS_MAX
    private var activeVideoSource: VideoSource? = null
    private val speedMenu: PlayerSpeedMenu by lazy {
        PlayerSpeedMenu(
            this,
            object : PlayerSpeedMenu.Callbacks {
                override fun currentSpeed(): Float = playbackSpeed

                override fun onSpeedSelected(speed: Float) {
                    setPlaybackSpeed(speed)
                }

                override fun onSpeedMenuDismissed() {
                    scheduleControlsAutoHide()
                }
            }
        )
    }
    private var playbackSpeed = PlaybackSpeedOptions.DEFAULT
    private val progressHandler = Handler(Looper.getMainLooper())
    private val controlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        hideControls()
    }
    private val progressRunnable = object : Runnable {
        override fun run() {
            updatePlaybackProgress()
            progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aniListId = intent.getIntExtra(EXTRA_ANILIST_ID, INVALID_ANILIST_ID)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        requestedEpisode = intent.getIntExtra(EXTRA_INITIAL_EPISODE, DEFAULT_INITIAL_EPISODE)
            .coerceAtLeast(DEFAULT_INITIAL_EPISODE)
        playbackSpeed = PlaybackSpeedOptions.normalize(playbackPreferencesStore.getSpeed())

        if (aniListId == INVALID_ANILIST_ID || title.isBlank()) {
            finish()
            return
        }

        currentAnime = PlaybackAnime(
            aniListId = aniListId,
            malId = intent.getIntExtra(EXTRA_MAL_ID, INVALID_MAL_ID).takeIf { it != INVALID_MAL_ID },
            title = title,
            coverImageUrl = intent.getStringExtra(EXTRA_COVER_IMAGE_URL).orEmpty(),
            bannerImageUrl = intent.getStringExtra(EXTRA_BANNER_IMAGE_URL).orEmpty(),
            totalEpisodes = intent.getIntExtra(EXTRA_TOTAL_EPISODES, INVALID_TOTAL_EPISODES)
                .takeIf { it != INVALID_TOTAL_EPISODES }
        )

        rebuildShell()
        attachEngine(playbackEngine)
        engineAttached = true
        loadPlayback()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        rebuildShell()

        if (engineAttached) {
            playbackEngine.setVideoView(playerView)
        }
    }

    override fun onStart() {
        super.onStart()
        if (engineAttached) playbackEngine.onForeground()
        startProgressLoop()
    }

    override fun onStop() {
        stopProgressLoop()
        cancelControlsAutoHide()
        speedMenu.dismiss(notifyCallbacks = false)
        if (engineAttached) playbackEngine.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressRunnable)
        controlsHandler.removeCallbacks(hideControlsRunnable)
        speedMenu.dismiss(notifyCallbacks = false)
        if (::shell.isInitialized) shell.controller.release()
        playbackDisposable?.dispose()
        if (engineAttached) {
            playbackEngine.release()
            engineAttached = false
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        finish()
    }

    private fun attachEngine(engine: PlaybackEngine) {
        engine.setVideoView(playerView)
        engine.listener = object : PlaybackEngineListener {
            override fun onStateChanged(state: PlaybackState) {
                latestPlaybackState = state
                when (state.readyState) {
                    PlaybackReadyState.BUFFERING -> {
                        transportVisible = true
                        if (controlsVisible) {
                            scheduleControlsAutoHide()
                        } else {
                            updateTransportControls()
                        }
                        showMessage(R.string.player_buffering, loading = true)
                    }
                    PlaybackReadyState.READY -> {
                        transportVisible = true
                        showControls()
                        if (!PlaybackSpeedOptions.isSelected(state.speed, playbackSpeed)) {
                            playbackEngine.setSpeed(playbackSpeed)
                        }
                        showMessage(R.string.player_playing)
                    }
                    PlaybackReadyState.ENDED -> {
                        transportVisible = true
                        showControls()
                        showMessage(R.string.player_ended)
                    }
                    PlaybackReadyState.IDLE -> {
                        transportVisible = false
                        hideControls()
                        updateTransportControls()
                    }
                }
            }

            override fun onError(error: PlaybackError) {
                showError()
            }
        }
    }

    private fun rebuildShell() {
        speedMenu.dismiss(notifyCallbacks = false)
        val callbacks = object : PlayerControllerSkeleton.Callbacks {
            override fun onBackClicked() {
                finish()
            }

            override fun onRetryClicked() {
                loadPlayback()
            }

            override fun onPlayPauseClicked() {
                togglePlayback()
            }

            override fun onPreviousEpisodeClicked() {
                loadAdjacentEpisode(-1)
            }

            override fun onNextEpisodeClicked() {
                loadAdjacentEpisode(1)
            }

            override fun onSeekFinished(fraction: Float) {
                seekToFraction(fraction)
            }

            override fun onLockClicked() {
                lockControls()
            }

            override fun onUnlockClicked() {
                unlockControls()
            }

            override fun onSpeedClicked() {
                showPlaybackSpeedMenu()
            }

            override fun onServerClicked() = Unit

            override fun onAudioClicked() = Unit

            override fun onSubtitleClicked() = Unit

            override fun onCastClicked() = Unit

            override fun onEpisodeClicked() = Unit

            override fun onSettingsClicked() = Unit

            override fun onVolumeClicked() = Unit

            override fun onRewindClicked() {
                seekBy(-GESTURE_SEEK_DURATION_MS)
            }

            override fun onForwardClicked() {
                seekBy(GESTURE_SEEK_DURATION_MS)
            }

            override fun onRotateClicked() {
                requestedOrientation = if (
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                ) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
        }
        val title = currentAnime?.title.orEmpty()
        val episodeLabel = getString(R.string.player_shell_status_format, title, requestedEpisode)

        shell = PlayerShellLayoutBuilder(this, callbacks).build(title, episodeLabel)
        val gestureHandler = PlayerGestureHandler(
            this,
            object : PlayerGestureHandler.Callbacks {
                override fun isControlsLocked(): Boolean = controlsLocked

                override fun onLockedTouch() {
                    shell.controller.showUnlockOverlayTemporarily()
                }

                override fun onSingleTap() {
                    handlePlayerTap()
                }

                override fun onSeek(isForward: Boolean) {
                    seekBy(if (isForward) GESTURE_SEEK_DURATION_MS else -GESTURE_SEEK_DURATION_MS)
                }

                override fun onPlaybackTouchStarted() {
                    scheduleControlsAutoHide()
                }

                override fun currentBrightness(): Float {
                    return playbackBrightness
                }

                override fun setBrightness(value: Float) {
                    playbackBrightness = value.coerceIn(BRIGHTNESS_MINIMUM, BRIGHTNESS_MAX)
                    applyBrightnessScrim()
                }

                override fun currentVolume(): Int {
                    return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }

                override fun maxVolume(): Int {
                    return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                }

                override fun setVolume(value: Int) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
                }

                override fun showHud(label: String, level: Float, isRightSide: Boolean) {
                    val layoutParams = shell.gestureHudView.layoutParams as FrameLayout.LayoutParams
                    layoutParams.gravity = (if (isRightSide) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
                    layoutParams.setMargins(
                        if (isRightSide) 0 else dp(GESTURE_HUD_SIDE_MARGIN_DP),
                        0,
                        if (isRightSide) dp(GESTURE_HUD_SIDE_MARGIN_DP) else 0,
                        0
                    )
                    shell.gestureHudView.layoutParams = layoutParams
                    shell.gestureHudView.setLevel(label, level)
                    shell.gestureHudView.animate().cancel()
                    shell.gestureHudView.alpha = 1f
                    shell.gestureHudView.visibility = View.VISIBLE
                }

                override fun hideHudSoon() {
                    shell.gestureHudView.animate().cancel()
                    shell.gestureHudView.animate()
                        .alpha(0f)
                        .setStartDelay(GESTURE_HUD_HIDE_START_DELAY_MS)
                        .setDuration(GESTURE_HUD_FADE_DURATION_MS)
                        .withEndAction { shell.gestureHudView.visibility = View.GONE }
                        .start()
                }
            }
        )
        this.gestureHandler = gestureHandler
        shell.videoFrame.setOnTouchListener(gestureHandler)
        playerView = shell.playerView
        applyBrightnessScrim()
        loadingIndicator = shell.loadingIndicator
        setContentView(shell.root)
        applySystemBars()
        shell.controller.setStatus(statusMessage, statusRetryVisible)
        shell.controller.setLocked(controlsLocked)
        loadingIndicator.visibility = if (waitingForPlayback) View.VISIBLE else View.GONE
        shell.controller.updateServerLabel(activeVideoSource?.server.orEmpty().ifBlank { "S1" })
        updateTransportControls()
        if (controlsVisible) scheduleControlsAutoHide()
    }

    private fun applySystemBars() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = if (isLandscape) {
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun loadPlayback() {
        val anime = currentAnime ?: return
        loadPlayback(anime)
    }

    private fun loadPlayback(anime: PlaybackAnime) {
        showMessage(R.string.player_loading_episode, arguments = listOf(anime.title, requestedEpisode), loading = true)
        playbackDisposable?.dispose()

        playbackDisposable = episodeRepository.getEpisodes(anime)
            .flatMap { episodes ->
                val selectedEpisode = episodeNavigator.reset(episodes, requestedEpisode)
                    ?: throw PlaybackUnavailableException()
                videoSourceRepository.getSources(anime, selectedEpisode)
                    .map { sources -> selectedEpisode to sources }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (episode, sources) -> startPlayback(episode, sources) },
                { error ->
                    if (error is PlaybackUnavailableException) showUnavailable() else showError()
                }
            )
    }

    private fun startPlayback(episode: PlaybackEpisode, sources: List<VideoSource>) {
        val source = sources.firstOrNull()
        if (source == null) {
            showUnavailable()
            return
        }

        val videoSource = sources.firstOrNull()
        activeVideoSource = videoSource
        latestPlaybackState = PlaybackState(speed = playbackSpeed)
        transportVisible = false
        showMessage(R.string.player_starting_playback, arguments = listOf(episode.name), loading = true)
        shell.controller.updateServerLabel(videoSource?.server.orEmpty().ifBlank { "S1" })
        playbackEngine.prepare(source)
        playbackEngine.setSpeed(playbackSpeed)
        playbackEngine.play()
    }

    private fun handlePlayerTap() {
        if (controlsLocked) {
            shell.controller.showUnlockOverlayTemporarily()
            return
        }
        toggleControlsVisibility()
    }

    private fun toggleControlsVisibility() {
        if (!transportVisible || controlsLocked) return
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        if (!transportVisible || controlsLocked) return
        controlsVisible = true
        updateTransportControls()
        scheduleControlsAutoHide()
    }

    private fun hideControls() {
        cancelControlsAutoHide()
        controlsVisible = false
        updateTransportControls()
    }

    private fun scheduleControlsAutoHide() {
        if (!controlsVisible || controlsLocked || !transportVisible) return
        controlsHandler.removeCallbacks(hideControlsRunnable)
        controlsHandler.postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_DELAY_MS)
    }

    private fun cancelControlsAutoHide() {
        controlsHandler.removeCallbacks(hideControlsRunnable)
    }

    private fun lockControls() {
        cancelControlsAutoHide()
        controlsVisible = false
        controlsLocked = true
        shell.controller.setLocked(true)
        updateTransportControls()
    }

    private fun unlockControls() {
        controlsLocked = false
        controlsVisible = true
        shell.controller.setLocked(false)
        updateTransportControls()
        scheduleControlsAutoHide()
    }

    private fun seekBy(deltaMs: Long) {
        if (!transportVisible || controlsLocked) return
        val durationMs = latestPlaybackState.durationMs
        if (durationMs <= 0L) return
        val targetPositionMs = (latestPlaybackState.positionMs + deltaMs)
            .coerceIn(0L, durationMs)
        playbackEngine.seekTo(targetPositionMs)
        latestPlaybackState = latestPlaybackState.copy(positionMs = targetPositionMs)
        updateTransportControls()
    }

    private val audioManager: AudioManager
        get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun applyBrightnessScrim() {
        if (!::shell.isInitialized) return
        val dimmingFraction = (1f - playbackBrightness).coerceIn(0f, 1f)
        shell.brightnessScrimView.alpha = dimmingFraction * BRIGHTNESS_SCRIM_MAXIMUM_ALPHA
    }

    private fun showPlaybackSpeedMenu() {
        if (!transportVisible || controlsLocked) return
        cancelControlsAutoHide()
        speedMenu.show()
    }

    private fun setPlaybackSpeed(speed: Float) {
        val normalizedSpeed = PlaybackSpeedOptions.normalize(speed)
        playbackSpeed = normalizedSpeed
        playbackPreferencesStore.setSpeed(normalizedSpeed)
        if (engineAttached) {
            playbackEngine.setSpeed(normalizedSpeed)
        }
        updateTransportControls()
    }

    private fun togglePlayback() {
        if (!transportVisible || controlsLocked) return
        if (latestPlaybackState.playWhenReady) playbackEngine.pause() else playbackEngine.play()
    }

    private fun updateTransportControls() {
        shell.controller.setTransportState(
            isVisible = transportVisible,
            controlsShown = controlsVisible,
            isPlaying = latestPlaybackState.playWhenReady,
            canShowPrevious = transportVisible && !controlsLocked && episodeNavigator.canMove(-1),
            canShowNext = transportVisible && !controlsLocked && episodeNavigator.canMove(1)
        )
        shell.controller.updatePlaybackProgress(
            positionMs = latestPlaybackState.positionMs,
            bufferedPositionMs = latestPlaybackState.bufferedPositionMs,
            durationMs = latestPlaybackState.durationMs
        )
    }

    private fun seekToFraction(fraction: Float) {
        if (!transportVisible || controlsLocked) return
        val durationMs = latestPlaybackState.durationMs
        if (durationMs <= 0L) return

        val positionMs = (durationMs * fraction).toLong().coerceIn(0L, durationMs)
        latestPlaybackState = latestPlaybackState.copy(positionMs = positionMs)
        playbackEngine.seekTo(positionMs)
        updateTransportControls()
    }

    private fun startProgressLoop() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun stopProgressLoop() {
        progressHandler.removeCallbacks(progressRunnable)
    }

    private fun updatePlaybackProgress() {
        if (!transportVisible || !engineAttached) return
        latestPlaybackState = playbackEngine.currentState
        updateTransportControls()
    }

    private fun loadAdjacentEpisode(offset: Int) {
        val anime = currentAnime ?: return
        if (!transportVisible || controlsLocked || !episodeNavigator.canMove(offset)) return
        val episode = episodeNavigator.move(offset) ?: return

        requestedEpisode = episodeNavigator.selectedIndex + 1
        latestPlaybackState = PlaybackState()
        transportVisible = false
        showMessage(R.string.player_loading_episode, arguments = listOf(anime.title, requestedEpisode), loading = true)
        playbackDisposable?.dispose()
        playbackDisposable = videoSourceRepository.getSources(anime, episode)
            .map { sources -> episode to sources }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (selectedEpisode, sources) -> startPlayback(selectedEpisode, sources) },
                { showError() }
            )
    }

    private fun showMessage(
        messageResId: Int,
        arguments: List<Any?> = emptyList(),
        retryVisible: Boolean = false,
        loading: Boolean = false
    ) {
        updateStatus(getString(messageResId, *arguments.toTypedArray()), retryVisible, loading)
    }

    private fun showUnavailable() {
        transportVisible = false
        showMessage(R.string.playback_unavailable, retryVisible = true)
    }

    private fun showError() {
        transportVisible = false
        showMessage(R.string.player_runtime_error, retryVisible = true)
    }

    private fun updateStatus(message: String, retryVisible: Boolean, loading: Boolean) {
        statusMessage = message
        statusRetryVisible = retryVisible
        waitingForPlayback = loading
        shell.controller.setStatus(statusMessage, statusRetryVisible)
        loadingIndicator.visibility = if (waitingForPlayback) View.VISIBLE else View.GONE
        updateTransportControls()
    }

    companion object {
        const val EXTRA_ANILIST_ID = "EXTRA_ANILIST_ID"
        const val GESTURE_SEEK_DURATION_MS = 10_000L
        const val GESTURE_HUD_SIDE_MARGIN_DP = PlayerShellMetrics.GESTURE_HUD_SIDE_MARGIN_DP
        const val GESTURE_HUD_HIDE_START_DELAY_MS = 420L
        const val GESTURE_HUD_FADE_DURATION_MS = 160L
        const val BRIGHTNESS_MINIMUM = 0.05f
        const val BRIGHTNESS_MAX = 1f
        const val BRIGHTNESS_SCRIM_MAXIMUM_ALPHA = 0.85f
        const val EXTRA_MAL_ID = "EXTRA_MAL_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_COVER_IMAGE_URL = "EXTRA_COVER_IMAGE_URL"
        const val EXTRA_BANNER_IMAGE_URL = "EXTRA_BANNER_IMAGE_URL"
        const val EXTRA_TOTAL_EPISODES = "EXTRA_TOTAL_EPISODES"
        const val EXTRA_INITIAL_EPISODE = "EXTRA_INITIAL_EPISODE"

        internal const val INVALID_ANILIST_ID = 0
        internal const val INVALID_MAL_ID = 0
        internal const val INVALID_TOTAL_EPISODES = 0
        internal const val DEFAULT_INITIAL_EPISODE = 1
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        internal const val CONTROLS_AUTO_HIDE_DELAY_MS = 4_000L
    }

    private class PlaybackUnavailableException : IllegalStateException()
}
