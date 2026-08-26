package msr.atsulab.app.player.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import java.io.File
import java.io.IOException
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.view.ViewGroup
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import msr.atsulab.app.R
import msr.atsulab.app.data.repository.MediaListRepository
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.helper.enums.Source
import msr.atsulab.app.player.domain.PlaybackSpeedOptions
import msr.atsulab.app.player.domain.SubtitleStyleOptions
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.PlaybackProgress
import msr.atsulab.app.player.domain.model.SkipInterval
import msr.atsulab.app.player.domain.model.SubtitleStyle
import msr.atsulab.app.player.domain.model.SubtitleTrack
import msr.atsulab.app.player.domain.model.VideoQuality
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.domain.repository.EpisodeRepository
import msr.atsulab.app.player.domain.repository.PlaybackProgressRepository
import msr.atsulab.app.player.domain.repository.SkipTimeRepository
import msr.atsulab.app.player.domain.repository.VideoSourceRepository
import msr.atsulab.app.player.engine.PlaybackEngine
import msr.atsulab.app.player.engine.PlaybackEngineListener
import msr.atsulab.app.player.engine.PlaybackReadyState
import msr.atsulab.app.player.engine.PlaybackError
import msr.atsulab.app.player.download.CompletedDownload
import msr.atsulab.app.player.download.DownloadEntryStore
import msr.atsulab.app.player.download.DownloadQueueStore
import msr.atsulab.app.player.download.DownloadRequest
import msr.atsulab.app.player.download.PlayerDownloadService
import msr.atsulab.app.player.engine.PlaybackState
import msr.atsulab.app.player.runtime.PlaybackEpisodeNavigator
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import msr.atsulab.app.player.storage.SourceMappingStore
import msr.atsulab.app.type.MediaListStatus
import org.koin.java.KoinJavaComponent.inject
import kotlin.math.roundToInt

class PlayerActivity : AppCompatActivity() {

    private val episodeRepository: EpisodeRepository by inject(EpisodeRepository::class.java)
    private val videoSourceRepository: VideoSourceRepository by inject(VideoSourceRepository::class.java)
    private val skipTimeRepository: SkipTimeRepository by inject(SkipTimeRepository::class.java)
    private val playbackProgressRepository: PlaybackProgressRepository by inject(PlaybackProgressRepository::class.java)
    private val playbackEngine: PlaybackEngine by inject(PlaybackEngine::class.java)
    private val playbackPreferencesStore: PlaybackPreferencesStore by inject(PlaybackPreferencesStore::class.java)
    private val sourceMappingStore: SourceMappingStore by inject(SourceMappingStore::class.java)
    private val downloadQueueStore: DownloadQueueStore by inject(DownloadQueueStore::class.java)
    private val downloadEntryStore: DownloadEntryStore by inject(DownloadEntryStore::class.java)
    private val mediaListRepository: MediaListRepository by inject(MediaListRepository::class.java)
    private val userRepository: UserRepository by inject(UserRepository::class.java)

    private val sourceSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadPlayback()
        }
    }

    private val frameCaptureManager by lazy {
        PlayerFrameCaptureManager(this, playbackPreferencesStore)
    }

    private val episodePanel: PlayerEpisodePanel by lazy {
        PlayerEpisodePanel(
            this,
            object : PlayerEpisodePanel.Callbacks {
                override fun episodes(): List<PlaybackEpisode> = episodeNavigator.episodes()

                override fun currentEpisode(): PlaybackEpisode? = episodeNavigator.currentEpisode

                override fun rangeStart(): Int = episodeRangeStart

                override fun onRangeSelected(rangeStart: Int) {
                    episodeRangeStart = rangeStart
                    episodePanel.refreshIfShowing()
                }

                override fun onEpisodeSelected(episode: PlaybackEpisode) {
                    loadSelectedEpisode(episode)
                }

                override fun onEpisodePanelDismissed() {
                    applySystemBars()
                    scheduleControlsAutoHide()
                }
            }
        )
    }

    private lateinit var shell: PlayerShellViews
    private lateinit var playerView: PlayerView
    private lateinit var loadingIndicator: ProgressBar

    private val episodeNavigator = PlaybackEpisodeNavigator()

    private var currentAnime: PlaybackAnime? = null
    private var requestedEpisode = DEFAULT_INITIAL_EPISODE
    private var engineAttached = false
    private var playbackDisposable: Disposable? = null
    private var moreServersDisposable: Disposable? = null
    private var skipTimesDisposable: Disposable? = null
    private var aniListSyncDisposable: Disposable? = null
    private var statusMessage = ""
    private var statusRetryVisible = false
    private var waitingForPlayback = true
    private var latestPlaybackState = PlaybackState()
    private var pendingResumePositionMs = 0L
    private var lastProgressSaveTimeMs = 0L
    private var transportVisible = false
    private var controlsVisible = false
    private var controlsLocked = false
    private var gestureHandler: PlayerGestureHandler? = null
    private var playbackBrightness = BRIGHTNESS_MAX
    private var activeVideoSource: VideoSource? = null
    private var offlineFilePath: String? = null
    private var offlineEpisodeName: String = ""
    private var availableVideoSources: List<VideoSource> = emptyList()
    private var selectedSourceIndex = INVALID_SOURCE_INDEX
    private var showDubSources = false
    private var areMoreServersLoading = false
    private var hasAllServersFailed = false
    private val failedSourceIndexes = mutableSetOf<Int>()
    private var skipIntervals: List<SkipInterval> = emptyList()
    private var skipFetchKey = ""
    private var selectedQualityTrackId: String? = null
    private var isLoadingVideoSources = true
    private var sourceErrorMessage: String? = null
    private var episodeRangeStart = DEFAULT_INITIAL_EPISODE
    private var isAutoNextRequested = false
    private val customFontPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) importCustomSubtitleFont(uri)
    }
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
    private val subtitleStylePanel: PlayerSubtitleStylePanel by lazy {
        PlayerSubtitleStylePanel(
            this,
            object : PlayerSubtitleStylePanel.Callbacks {
                override fun currentSubtitleStyle(): SubtitleStyle = playbackPreferencesStore.getSubtitleStyle()

                override fun onSubtitleStyleChanged(style: SubtitleStyle) {
                    playbackPreferencesStore.setSubtitleStyle(style)
                    applySubtitleStyle(style)
                }

                override fun onCustomFontClicked() {
                    subtitleStylePanel.dismiss(notifyCallbacks = false)
                    customFontPicker.launch("*/*")
                }

                override fun onClearCustomFontClicked() {
                    clearCustomSubtitleFont()
                }

                override fun onSubtitleStyleDismissed() {
                    scheduleControlsAutoHide()
                }
            }
        )
    }
    private val subtitleMenu: PlayerSubtitleMenu by lazy {
        PlayerSubtitleMenu(
            this,
            object : PlayerSubtitleMenu.Callbacks {
                override fun subtitleTracks(): List<SubtitleTrack> = latestPlaybackState.subtitleTracks

                override fun hasExternalSubtitle(): Boolean =
                    activeVideoSource?.subtitleUrl?.isNotBlank() == true

                override fun onSubtitleSelected(trackId: String?) {
                    if (engineAttached) playbackEngine.setSubtitleTrack(trackId)
                }

                override fun onStyleSettingsClicked() {
                    dismissSubtitleMenus()
                    subtitleStylePanel.show()
                }

                override fun onSubtitleMenuDismissed() {
                    applySystemBars()
                    scheduleControlsAutoHide()
                }
            }
        )
    }
    private val frameCaptureSettingsMenu: PlayerFrameCaptureSettingsMenu by lazy {
        PlayerFrameCaptureSettingsMenu(
            this,
            object : PlayerFrameCaptureSettingsMenu.Callbacks {
                override fun isEnabled(): Boolean =
                    playbackPreferencesStore.isFrameCaptureEnabled()

                override fun isAlwaysVisible(): Boolean =
                    playbackPreferencesStore.isFrameCaptureAlwaysVisible()

                override fun onEnabledChanged(enabled: Boolean) {
                    playbackPreferencesStore.setFrameCaptureEnabled(enabled)
                    if (::shell.isInitialized) shell.controller.setFrameCaptureEnabled(enabled)
                }

                override fun onAlwaysVisibleChanged(enabled: Boolean) {
                    playbackPreferencesStore.setFrameCaptureAlwaysVisible(enabled)
                    if (::shell.isInitialized) {
                        shell.controller.setFrameCaptureAlwaysVisible(enabled)
                    }
                }

                override fun onDownloadEpisodeClicked() {
                    frameCaptureSettingsMenu.dismiss()
                    downloadActiveEpisode()
                }

                override fun onFixSourceMatchClicked() {
                    frameCaptureSettingsMenu.dismiss()
                    openSourceSelection()
                }

                override fun onSettingsDismissed() {
                    applySystemBars()
                    scheduleControlsAutoHide()
                }
            }
        )
    }
    private val qualityMenu: PlayerQualityMenu by lazy {
        PlayerQualityMenu(
            this,
            object : PlayerQualityMenu.Callbacks {
                override fun videoQualities(): List<VideoQuality> = latestPlaybackState.videoQualities

                override fun selectedQualityTrackId(): String? = selectedQualityTrackId

                override fun onQualitySelected(trackId: String?) {
                    selectVideoQuality(trackId)
                }

                override fun onQualityMenuDismissed() {
                    applySystemBars()
                    scheduleControlsAutoHide()
                }
            }
        )
    }
    private val serverMenu: PlayerServerMenu by lazy {
        PlayerServerMenu(
            this,
            object : PlayerServerMenu.Callbacks {
                override fun videoSources(): List<VideoSource> = availableVideoSources

                override fun selectedSourceIndex(): Int = selectedSourceIndex

                override fun showDub(): Boolean = showDubSources

                override fun onLanguageModeSelected(showDub: Boolean) {
                    selectLanguageMode(showDub)
                }

                override fun onServerSelected(sourceIndex: Int) {
                    selectVideoSource(sourceIndex)
                }

                override fun isMoreServersLoading(): Boolean = areMoreServersLoading

                override fun hasAllServersFailed(): Boolean = hasAllServersFailed

                override fun onRetryServersClicked() {
                    retryServerSources()
                }

                override fun onServerMenuDismissed() {
                    applySystemBars()
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
        offlineFilePath = intent.getStringExtra(EXTRA_OFFLINE_FILE_PATH).orEmpty().takeIf { it.isNotBlank() }
        offlineEpisodeName = intent.getStringExtra(EXTRA_OFFLINE_EPISODE_NAME).orEmpty()
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
        episodePanel.dismiss(notifyCallbacks = false)
        rebuildShell()

        if (engineAttached) {
            playbackEngine.setVideoView(playerView)
        }
    }

    override fun onPause() {
        super.onPause()
        savePlaybackProgress()
    }

    override fun onStart() {
        super.onStart()
        if (engineAttached) playbackEngine.onForeground()
        frameCaptureManager.start()
        startProgressLoop()
    }

    override fun onStop() {
        stopProgressLoop()
        frameCaptureManager.stop()
        cancelControlsAutoHide()
        speedMenu.dismiss(notifyCallbacks = false)
        frameCaptureSettingsMenu.dismiss(notifyCallbacks = false)
        dismissSubtitleMenus()
        serverMenu.dismiss(notifyCallbacks = false)
        episodePanel.dismiss(notifyCallbacks = false)
        shell.portraitContent?.dismissPopups()
        if (engineAttached) playbackEngine.onBackground()
        savePlaybackProgress(force = true)
        super.onStop()
    }

    override fun onDestroy() {
        savePlaybackProgress(force = true)
        progressHandler.removeCallbacks(progressRunnable)
        controlsHandler.removeCallbacks(hideControlsRunnable)
        speedMenu.dismiss(notifyCallbacks = false)
        frameCaptureSettingsMenu.dismiss(notifyCallbacks = false)
        dismissSubtitleMenus()
        serverMenu.dismiss(notifyCallbacks = false)
        qualityMenu.dismiss(notifyCallbacks = false)
        episodePanel.dismiss(notifyCallbacks = false)
        if (::shell.isInitialized) shell.portraitContent?.dismissPopups()
        if (::shell.isInitialized) shell.controller.release()
        playbackDisposable?.dispose()
        moreServersDisposable?.dispose()
        skipTimesDisposable?.dispose()
        aniListSyncDisposable?.dispose()
        if (engineAttached) {
            playbackEngine.release()
            engineAttached = false
        }
        frameCaptureManager.release()
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
                        if (!state.playWhenReady) {
                            savePlaybackProgress(force = true)
                        } else {
                            persistPlaybackProgressIfDue(state)
                        }
                        if (!PlaybackSpeedOptions.isSelected(state.speed, playbackSpeed)) {
                            playbackEngine.setSpeed(playbackSpeed)
                        }
                        hasAllServersFailed = false
                        failedSourceIndexes.remove(selectedSourceIndex)
                        showMessage(R.string.player_playing)
                        loadSkipTimes(state.durationMs)
                    }
                    PlaybackReadyState.ENDED -> {
                        transportVisible = true
                        showControls()
                        showMessage(R.string.player_ended)
                        savePlaybackProgress(force = true)
                        playNextEpisodeIfAvailable()
                    }
                    PlaybackReadyState.IDLE -> {
                        transportVisible = false
                        hideControls()
                        updateTransportControls()
                    }
                }
            }

            override fun onError(error: PlaybackError) {
                handlePlaybackError()
            }
        }
    }

    private fun rebuildShell() {
        if (::shell.isInitialized) shell.portraitContent?.dismissPopups()
        speedMenu.dismiss(notifyCallbacks = false)
        frameCaptureSettingsMenu.dismiss(notifyCallbacks = false)
        episodePanel.dismiss(notifyCallbacks = false)
        dismissSubtitleMenus()
        serverMenu.dismiss(notifyCallbacks = false)
        qualityMenu.dismiss(notifyCallbacks = false)
        val callbacks = object : PlayerShellCallbacks {
            override fun onBackClicked() {
                finish()
            }

            override fun onRetryClicked() {
                loadPlayback()
            }

            override fun onFixSourceMatchClicked() {
                openSourceSelection()
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

            override fun onQualityClicked() {
                showQualityMenu()
            }

            override fun onServerClicked() {
                showServerMenu(showLanguageTabs = false)
            }

            override fun onAudioClicked() {
                showServerMenu(showLanguageTabs = true)
            }

            override fun onSubtitleClicked() {
                showSubtitleMenu()
            }

            override fun onCastClicked() = Unit

            override fun onEpisodeClicked() {
                showEpisodePanel()
            }

            override fun onSettingsClicked() {
                showFrameCaptureSettings()
            }

            override fun onFrameCaptureClicked() {
                captureCurrentFrame()
            }

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

            override fun currentState(): PlayerPortraitUiState {
                return PlayerPortraitUiState(
                    episodes = episodeNavigator.episodes(),
                    currentEpisode = episodeNavigator.currentEpisode,
                    rangeStart = episodeRangeStart,
                    showDub = showDubSources,
                    sources = availableVideoSources,
                    selectedSourceIndex = selectedSourceIndex,
                    isLoadingSources = isLoadingVideoSources,
                    isMoreServersLoading = areMoreServersLoading,
                    hasAllServersFailed = hasAllServersFailed
                )
            }

            override fun onLanguageModeSelected(showDub: Boolean) {
                selectLanguageMode(showDub)
                refreshPortraitContent()
            }

            override fun onServerSelected(sourceIndex: Int) {
                selectVideoSource(sourceIndex)
                refreshPortraitContent()
            }

            override fun onEpisodeSelected(episode: PlaybackEpisode) {
                loadSelectedEpisode(episode)
            }

            override fun onRangeSelected(rangeStart: Int) {
                episodeRangeStart = rangeStart
                refreshPortraitContent()
            }
        }
        val title = currentAnime?.title.orEmpty()
        val episodeLabel = getString(R.string.player_shell_status_format, title, requestedEpisode)

        shell = PlayerShellLayoutBuilder(this, callbacks).build(title, episodeLabel)
        shell.controller.setFrameCaptureEnabled(
            playbackPreferencesStore.isFrameCaptureEnabled()
        )
        shell.controller.setFrameCaptureAlwaysVisible(
            playbackPreferencesStore.isFrameCaptureAlwaysVisible()
        )
        shell.captureButton?.let { captureButton ->
            frameCaptureManager.attachCaptureButton(shell.videoFrame, captureButton) {
                captureCurrentFrame()
            }
        }
        shell.skipButton.setOnClickListener { skipActiveSection() }
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
        applySubtitleStyle(playbackPreferencesStore.getSubtitleStyle())
        applyBrightnessScrim()
        loadingIndicator = shell.loadingIndicator
        setContentView(shell.root)
        refreshPortraitContent()
        applySystemBars()
        shell.controller.setStatus(statusMessage, statusRetryVisible)
        shell.controller.setLocked(controlsLocked)
        loadingIndicator.visibility = if (waitingForPlayback) View.VISIBLE else View.GONE
        shell.controller.updateServerLabel(
            PlayerServerMenuModel.controlLabel(availableVideoSources, selectedSourceIndex)
        )
        shell.controller.updateQualityLabel(
            PlayerQualityMenuModel.controlLabel(latestPlaybackState.videoQualities, selectedQualityTrackId)
        )
        updateTransportControls()
        if (controlsVisible) scheduleControlsAutoHide()
        updateSkipViews()
        refreshPortraitContent()
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
        if (offlineFilePath != null) {
            startOfflinePlayback(anime)
            return
        }
        loadPlayback(anime)
    }

    private fun downloadActiveEpisode() {
        val anime = currentAnime ?: return
        val source = activeVideoSource ?: return
        val request = DownloadRequest(
            aniListId = anime.aniListId,
            episodeId = requestedEpisode.toString(),
            displayName = anime.title,
            url = source.url,
            quality = source.quality,
            referer = source.referer
        )
        val existing = downloadEntryStore.findByEpisode(anime.aniListId, requestedEpisode.toString())
        if (existing?.file?.exists() == true) {
            Toast.makeText(this, R.string.download_already_exists, Toast.LENGTH_SHORT).show()
            return
        }
        PlayerDownloadService.start(this, downloadQueueStore, listOf(request))
        Toast.makeText(this, R.string.download_queued, Toast.LENGTH_SHORT).show()
    }

    private fun openSourceSelection() {
        val anime = currentAnime ?: return
        sourceSelectionLauncher.launch(
            Intent(this, SourceSelectionActivity::class.java).apply {
                putExtra(SourceSelectionActivity.EXTRA_TITLE, anime.title)
                putExtra(SourceSelectionActivity.EXTRA_ANILIST_ID, anime.aniListId)
            }
        )
    }

    private fun startOfflinePlayback(anime: PlaybackAnime) {
        val filePath = offlineFilePath ?: return
        val file = File(filePath)
        if (!file.exists()) {
            showMessage(R.string.downloads_missing_file, retryVisible = false)
            return
        }

        val source = VideoSource(
            quality = "Offline",
            url = Uri.fromFile(file).toString(),
            displayName = "Offline"
        )
        activeVideoSource = source
        selectedQualityTrackId = null
        availableVideoSources = listOf(source)
        latestPlaybackState = PlaybackState(speed = playbackSpeed)
        transportVisible = false
        val episodeLabel = offlineEpisodeName.ifBlank { getString(R.string.player_episode_number_format, requestedEpisode) }
        showMessage(R.string.player_starting_playback, arguments = listOf(episodeLabel), loading = true)
        shell.watchingView?.text = getString(
            R.string.player_watching_episode,
            anime.title,
            requestedEpisode
        )
        shell.controller.updateServerLabel(getString(R.string.player_offline))
        shell.controller.updateQualityLabel("MP4")
        shell.portraitContent?.setSourceError(null)
        refreshPortraitContent()
        pendingResumePositionMs = restoreResumePosition(
            aniListId = anime.aniListId,
            playbackId = "offline-$requestedEpisode",
            episodeUrl = file.absolutePath,
            episodeNumber = requestedEpisode.toFloat()
        )
        playbackEngine.prepare(source, pendingResumePositionMs)
        playbackEngine.setSpeed(playbackSpeed)
        playbackEngine.play()
    }

    private fun loadPlayback(anime: PlaybackAnime) {
        showMessage(R.string.player_loading_episode, arguments = listOf(anime.title, requestedEpisode), loading = true)
        playbackDisposable?.dispose()
        moreServersDisposable?.dispose()
        areMoreServersLoading = false
        hasAllServersFailed = false
        failedSourceIndexes.clear()
        resetSkipTimes()
        markSourcesLoading()

        playbackDisposable = episodeRepository.getEpisodes(anime)
            .flatMap { episodes ->
                val selectedEpisode = episodeNavigator.reset(episodes, requestedEpisode)
                    ?: throw PlaybackUnavailableException()
                updateEpisodeSelectionState()
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
        isAutoNextRequested = false
        isLoadingVideoSources = false
        sourceErrorMessage = null
        availableVideoSources = sources
        areMoreServersLoading = false
        hasAllServersFailed = false
        failedSourceIndexes.clear()
        selectedSourceIndex = 0
        activeVideoSource = videoSource
        showDubSources = videoSource?.let(PlayerServerMenuModel::isDub) == true
        selectedQualityTrackId = null
        latestPlaybackState = PlaybackState(speed = playbackSpeed)
        transportVisible = false
        showMessage(R.string.player_starting_playback, arguments = listOf(episode.name), loading = true)
        shell.watchingView?.text = getString(
            R.string.player_watching_episode,
            currentAnime?.title.orEmpty(),
            episode.number.roundToInt().coerceAtLeast(0)
        )
        shell.controller.updateServerLabel(
            PlayerServerMenuModel.controlLabel(sources, selectedSourceIndex)
        )
        shell.controller.updateQualityLabel("AUTO")
        shell.portraitContent?.setSourceError(null)
        refreshPortraitContent()
        pendingResumePositionMs = restoreResumePosition(episode)
        playbackEngine.prepare(source, pendingResumePositionMs)
        playbackEngine.setSpeed(playbackSpeed)
        playbackEngine.play()
        loadMoreSources(episode)
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

    private val customSubtitleFontFile: File
        get() = File(File(filesDir, CUSTOM_SUBTITLE_FONT_DIRECTORY), CUSTOM_SUBTITLE_FONT_FILE_NAME)

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

    private fun showSubtitleMenu() {
        if (!transportVisible || controlsLocked) return
        cancelControlsAutoHide()
        subtitleMenu.show()
    }

    private fun showQualityMenu() {
        if (!transportVisible || controlsLocked) return
        cancelControlsAutoHide()
        qualityMenu.show()
    }

    private fun loadMoreSources(episode: PlaybackEpisode) {
        if (areMoreServersLoading) return
        if (!engineAttached) return
        val anime = currentAnime ?: return

        moreServersDisposable?.dispose()
        areMoreServersLoading = true
        moreServersDisposable = videoSourceRepository.getMoreSources(anime, episode)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { additionalSources -> finishLoadingMoreServers(additionalSources) },
                { finishLoadingMoreServers(emptyList()) }
            )
}

    private fun loadSkipTimes(durationMs: Long) {
        val anime = currentAnime ?: return
        val episode = episodeNavigator.currentEpisode ?: return
        if (durationMs <= 0L) return

        val seconds = durationMs / 1000L
        val fetchKey = "${anime.aniListId}|${episode.url}|${episode.number.toInt()}|$seconds"
        if (fetchKey == skipFetchKey) return

        skipTimesDisposable?.dispose()
        skipFetchKey = fetchKey
        skipIntervals = emptyList()
        updateSkipViews()
        skipTimesDisposable = skipTimeRepository.getSkipIntervals(anime, episode, durationMs)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { intervals ->
                    if (skipFetchKey == fetchKey) {
                        skipIntervals = intervals
                        updateSkipViews()
                    }
                },
                {
                    if (skipFetchKey == fetchKey) {
                        skipIntervals = emptyList()
                        updateSkipViews()
                    }
                }
            )
    }

    private fun resetSkipTimes() {
        skipTimesDisposable?.dispose()
        skipTimesDisposable = null
        skipFetchKey = ""
        skipIntervals = emptyList()
        if (::shell.isInitialized) updateSkipViews()
    }

    private fun skipActiveSection() {
        val interval = PlayerSkipController.activeInterval(
            skipIntervals,
            latestPlaybackState.positionMs
        ) ?: return

        playbackEngine.seekTo(interval.endMs)
        latestPlaybackState = latestPlaybackState.copy(positionMs = interval.endMs)
        if (!latestPlaybackState.playWhenReady) playbackEngine.play()
        updateTransportControls()
    }

    private fun showFrameCaptureSettings() {
        if (!transportVisible || controlsLocked) return
        cancelControlsAutoHide()
        frameCaptureSettingsMenu.show()
    }

    private fun captureCurrentFrame() {
        val allowWhileLocked = playbackPreferencesStore.isFrameCaptureAlwaysVisible()
        if (!transportVisible || (controlsLocked && !allowWhileLocked)) return
        val anime = currentAnime ?: return
        val episode = episodeNavigator.currentEpisode
        frameCaptureManager.captureCurrentFrame(
            playerView = playerView,
            playbackState = latestPlaybackState,
            animeTitle = anime.title,
            episodeLabel = episode?.name ?: requestedEpisode.toString()
        )
    }

    private fun updateSkipViews() {
        if (!::shell.isInitialized) return
        shell.controller.skipMarkerView.setData(skipIntervals, latestPlaybackState.durationMs)

        val activeInterval = PlayerSkipController.activeInterval(
            skipIntervals,
            latestPlaybackState.positionMs
        )
        shell.skipButton.text = activeInterval
            ?.let(PlayerSkipController::titleResource)
            ?.let { resource -> getString(resource) }
            .orEmpty()
        shell.skipButton.visibility = if (
            activeInterval != null &&
            transportVisible &&
            controlsVisible &&
            !controlsLocked
        ) View.VISIBLE else View.GONE
    }

    private fun finishLoadingMoreServers(additionalSources: List<VideoSource>) {
        val knownUrls = availableVideoSources.mapTo(mutableSetOf(), VideoSource::url)
        val uniqueSources = additionalSources.filter { source ->
            source.url.isNotBlank() && knownUrls.add(source.url)
        }
        if (uniqueSources.isNotEmpty()) {
            availableVideoSources += uniqueSources
        }
        areMoreServersLoading = false
        serverMenu.refreshIfShowing()
        refreshPortraitContent()

        if (selectedSourceIndex in failedSourceIndexes) {
            tryRecoverFromFailedSource()
        }
    }

    private fun retryServerSources() {
        hasAllServersFailed = false
        areMoreServersLoading = false
        failedSourceIndexes.clear()
        loadPlayback()
    }

    private fun handlePlaybackError() {
        if (availableVideoSources.isEmpty() || selectedSourceIndex !in availableVideoSources.indices) {
            showError()
            return
        }

        failedSourceIndexes += selectedSourceIndex
        if (areMoreServersLoading) {
            showMessage(R.string.player_trying_another_server)
            return
        }
        tryRecoverFromFailedSource()
    }

    private fun tryRecoverFromFailedSource() {
        val fallbackIndex = PlayerServerMenuModel.fallbackSourceIndex(
            sources = availableVideoSources,
            currentSourceIndex = selectedSourceIndex,
            failedSourceIndexes = failedSourceIndexes,
            showDub = showDubSources
        )
        if (fallbackIndex >= 0) {
            selectVideoSource(fallbackIndex)
            return
        }
        markAllServersFailed()
    }

    private fun markAllServersFailed() {
        hasAllServersFailed = true
        areMoreServersLoading = false
        transportVisible = false
        serverMenu.refreshIfShowing()
        setSourceError(getString(R.string.player_no_working_source))
        showMessage(R.string.player_no_working_source, retryVisible = true)
    }

    private fun markSourcesLoading() {
        isLoadingVideoSources = true
        sourceErrorMessage = null
        availableVideoSources = emptyList()
        selectedSourceIndex = INVALID_SOURCE_INDEX
        activeVideoSource = null
        refreshPortraitContent()
        shell.portraitContent?.setSourceError(null)
    }

    private fun setSourceError(message: String) {
        sourceErrorMessage = message
        shell.portraitContent?.setSourceError(message)
        refreshPortraitContent()
    }

    private fun refreshPortraitContent() {
        if (!::shell.isInitialized) return
        val content = shell.portraitContent ?: return
        content.render(portraitUiState())
        content.setSourceError(sourceErrorMessage)
    }

    private fun portraitUiState(): PlayerPortraitUiState {
        return PlayerPortraitUiState(
            episodes = episodeNavigator.episodes(),
            currentEpisode = episodeNavigator.currentEpisode,
            rangeStart = episodeRangeStart,
            showDub = showDubSources,
            sources = availableVideoSources,
            selectedSourceIndex = selectedSourceIndex,
            isLoadingSources = isLoadingVideoSources,
            isMoreServersLoading = areMoreServersLoading,
            hasAllServersFailed = hasAllServersFailed
        )
    }

    private fun showServerMenu(showLanguageTabs: Boolean) {
        if (!transportVisible || controlsLocked || availableVideoSources.isEmpty()) return
        cancelControlsAutoHide()
        serverMenu.show(showLanguageTabs)
    }

    private fun selectLanguageMode(showDub: Boolean) {
        val preferredIndex = PlayerServerMenuModel.preferredSourceIndex(
            sources = availableVideoSources,
            currentSourceIndex = selectedSourceIndex,
            showDub = showDub
        )
        if (preferredIndex < 0 || preferredIndex >= availableVideoSources.size) {
            showToast(if (showDub) R.string.player_no_dub_servers else R.string.player_no_sub_servers)
            return
        }

        showDubSources = showDub
        if (preferredIndex != selectedSourceIndex) {
            selectVideoSource(preferredIndex)
        } else {
            refreshPortraitContent()
        }
    }

    private fun selectVideoSource(sourceIndex: Int) {
        val source = availableVideoSources.getOrNull(sourceIndex) ?: return
        if (sourceIndex == selectedSourceIndex || !engineAttached) return

        savePlaybackProgress(force = true)
        val resumePositionMs = latestPlaybackState.positionMs.coerceAtLeast(0L)
        selectedSourceIndex = sourceIndex
        activeVideoSource = source
        showDubSources = PlayerServerMenuModel.isDub(source)
        selectedQualityTrackId = null
        latestPlaybackState = PlaybackState(speed = playbackSpeed, positionMs = resumePositionMs)
        shell.controller.updateServerLabel(PlayerServerMenuModel.controlLabel(availableVideoSources, sourceIndex))
        shell.controller.updateQualityLabel("AUTO")
        refreshPortraitContent()
        playbackEngine.prepare(source, resumePositionMs)
        playbackEngine.setSpeed(playbackSpeed)
        playbackEngine.play()
    }

    private fun selectVideoQuality(trackId: String?) {
        val isValidSelection = trackId == null ||
            latestPlaybackState.videoQualities.any { it.id == trackId }
        if (!engineAttached || !isValidSelection) return

        selectedQualityTrackId = trackId
        playbackEngine.setVideoQuality(trackId)
        shell.controller.updateQualityLabel(
            PlayerQualityMenuModel.controlLabel(latestPlaybackState.videoQualities, trackId)
        )
    }

    private fun dismissSubtitleMenus() {
        subtitleMenu.dismiss(notifyCallbacks = false)
        subtitleStylePanel.dismiss(notifyCallbacks = false)
    }

    private fun importCustomSubtitleFont(uri: Uri) {
        val targetFile = customSubtitleFontFile
        val temporaryFile = File(cacheDir, CUSTOM_SUBTITLE_FONT_TEMP_NAME)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                temporaryFile.outputStream().use(input::copyTo)
            } ?: throw IOException("Selected font stream unavailable")

            runCatching { Typeface.createFromFile(temporaryFile) }
                .onFailure { throw IOException("Selected file is not a supported font", it) }

            targetFile.parentFile?.mkdirs()
            if (targetFile.exists() && !targetFile.delete()) throw IOException("Existing font could not be replaced")
            if (!temporaryFile.renameTo(targetFile)) throw IOException("Selected font could not be saved")

            val style = playbackPreferencesStore.getSubtitleStyle()
                .copy(customFontPath = targetFile.absolutePath)
            playbackPreferencesStore.setSubtitleStyle(style)
            applySubtitleStyle(style)
            showToast(R.string.player_custom_subtitle_font_added)
        } catch (_: Exception) {
            temporaryFile.delete()
            showToast(R.string.player_invalid_subtitle_font)
        }
        refreshSubtitleStylePanelAfterPicker()
    }

    private fun clearCustomSubtitleFont() {
        val style = playbackPreferencesStore.getSubtitleStyle().copy(customFontPath = "")
        playbackPreferencesStore.setSubtitleStyle(style)
        customSubtitleFontFile.delete()
        applySubtitleStyle(style)
        showToast(R.string.player_custom_subtitle_font_cleared)
        refreshSubtitleStylePanelAfterPicker()
    }

    private fun refreshSubtitleStylePanelAfterPicker() {
        if (transportVisible && !controlsLocked) subtitleStylePanel.show()
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun applySubtitleStyle(style: SubtitleStyle) {
        if (!::playerView.isInitialized) return
        val subtitleView = playerView.subtitleView ?: return
        val normalizedStyle = SubtitleStyleOptions.normalize(style)
        subtitleView.setFractionalTextSize(
            SubtitleStyleOptions.BASE_FRACTIONAL_TEXT_SIZE * normalizedStyle.fontSize
        )
        val typeface = when (normalizedStyle.fontStyle) {
            SubtitleStyle.FONT_STYLE_BOLD -> Typeface.DEFAULT_BOLD
            SubtitleStyle.FONT_STYLE_ITALIC -> Typeface.defaultFromStyle(Typeface.ITALIC)
            SubtitleStyle.FONT_STYLE_BOLD_ITALIC -> Typeface.defaultFromStyle(Typeface.BOLD_ITALIC)
            else -> Typeface.DEFAULT
        }
        val effectiveTypeface = normalizedStyle.customFontPath.takeIf(String::isNotBlank)
            ?.let { path -> runCatching { Typeface.createFromFile(path) }.getOrNull() }
            ?: typeface
        val captionStyle = CaptionStyleCompat(
            normalizedStyle.fontColor,
            SubtitleStyleOptions.backgroundArgb(normalizedStyle),
            Color.TRANSPARENT,
            if (normalizedStyle.shadow > 0) CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW else CaptionStyleCompat.EDGE_TYPE_NONE,
            SubtitleStyleOptions.edgeArgb(normalizedStyle.shadow),
            effectiveTypeface
        )
        subtitleView.setStyle(captionStyle)
        (subtitleView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
            layoutParams.bottomMargin = dp((normalizedStyle.bottomPadding * 1.5f).roundToInt())
            subtitleView.layoutParams = layoutParams
        }
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
        updateSkipViews()
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
        persistPlaybackProgressIfDue(latestPlaybackState)
        updateTransportControls()
    }

    private fun restoreResumePosition(episode: PlaybackEpisode): Long {
        val anime = currentAnime ?: return 0L
        return restoreResumePosition(
            aniListId = anime.aniListId,
            playbackId = episode.playbackId.ifBlank { episode.url },
            episodeUrl = episode.url,
            episodeNumber = episode.number
        )
    }

    private fun restoreResumePosition(
        aniListId: Int,
        playbackId: String,
        episodeUrl: String,
        episodeNumber: Float
    ): Long {
        val saved = runCatching {
            playbackProgressRepository.find(aniListId, playbackId, episodeUrl)
        }.getOrNull() ?: return 0L
        if (saved.isConsideredWatched()) {
            playbackProgressRepository.remove(aniListId, playbackId, episodeUrl).subscribe()
            return 0L
        }
        return saved.positionMs.coerceIn(0L, maxOf(saved.durationMs - 1L, 0L))
            .takeIf { position -> position > MIN_RESUME_POSITION_MS && episodeNumber >= 0f }
            ?: 0L
    }

    private fun persistPlaybackProgressIfDue(state: PlaybackState) {
        val now = System.currentTimeMillis()
        if (!state.playWhenReady || state.durationMs <= 0L) return
        if (now - lastProgressSaveTimeMs < PROGRESS_SAVE_INTERVAL_MS) return
        savePlaybackProgress()
    }

    private fun savePlaybackProgress(force: Boolean = false) {
        val state = latestPlaybackState
        if (state.durationMs <= 0L || (!force && !engineAttached)) return
        val progress = buildPlaybackProgress(state) ?: return
        lastProgressSaveTimeMs = System.currentTimeMillis()
        runCatching { playbackProgressRepository.upsert(progress).subscribe() }
        if (progress.isConsideredWatched()) {
            syncAniListProgress(progress.episodeNumber.toInt().coerceAtLeast(1))
        }
    }

    private fun syncAniListProgress(watchedEpisode: Int) {
        val anime = currentAnime ?: return
        if (anime.aniListId <= 0 || watchedEpisode <= 0) return
        aniListSyncDisposable?.dispose()
        aniListSyncDisposable = userRepository.getViewer(Source.CACHE)
            .flatMap { viewer ->
                mediaListRepository.getMediaListCollection(Source.CACHE, viewer, MediaType.ANIME)
            }
            .map { collection ->
                collection.lists.asSequence()
                    .flatMap { it.entries.asSequence() }
                    .firstOrNull { it.media.getId() == anime.aniListId }
            }
            .flatMap { entry ->
                if (entry == null) {
                    mediaListRepository.updateMediaListStatus(MediaType.ANIME, anime.aniListId, MediaListStatus.CURRENT)
                } else {
                    val current = entry.progress ?: 0
                    if (watchedEpisode > current) {
                        mediaListRepository.updateMediaListProgress(
                            MediaType.ANIME,
                            entry.id ?: 0,
                            if (entry.status == MediaListStatus.PLANNING) MediaListStatus.CURRENT else null,
                            null,
                            watchedEpisode,
                            null
                        )
                    } else {
                        io.reactivex.rxjava3.core.Observable.just(entry)
                    }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {},
                { it.printStackTrace() }
            )
    }

    private fun buildPlaybackProgress(state: PlaybackState): PlaybackProgress? {
        val anime = currentAnime ?: return null
        val source = activeVideoSource ?: return null
        val episode = episodeNavigator.currentEpisode
        val isOffline = offlineFilePath != null
        val playbackId = if (isOffline) {
            "offline-$requestedEpisode"
        } else {
            episode?.playbackId?.ifBlank { episode.url }.orEmpty()
        }
        val episodeUrl = if (isOffline) {
            offlineFilePath.orEmpty()
        } else {
            episode?.url.orEmpty().ifBlank { source.url }
        }
        if (playbackId.isBlank() || episodeUrl.isBlank()) return null

        return PlaybackProgress(
            aniListId = anime.aniListId,
            playbackId = playbackId,
            episodeUrl = episodeUrl,
            animeTitle = anime.title,
            thumbnailImageUrl = episode?.thumbnailUrl ?: anime.coverImageUrl,
            bannerImageUrl = anime.bannerImageUrl,
            episodeName = if (isOffline) {
                offlineEpisodeName.ifBlank { getString(R.string.player_episode_number_format, requestedEpisode) }
            } else {
                episode?.name.orEmpty().ifBlank {
                    getString(R.string.player_episode_number_format, requestedEpisode)
                }
            },
            episodeNumber = if (isOffline) {
                requestedEpisode.toFloat()
            } else {
                episode?.number ?: requestedEpisode.toFloat()
            },
            sourceId = source.providerId ?: source.legacySourceId,
            sourceDisplayName = source.displayName.ifBlank { source.server },
            quality = source.quality,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun showEpisodePanel() {
        if (
            PlayerShellOrientation.fromConfiguration(resources.configuration) != PlayerShellOrientation.LANDSCAPE ||
            episodeNavigator.currentEpisode == null
        ) {
            return
        }
        cancelControlsAutoHide()
        controlsVisible = true
        updateTransportControls()
        episodePanel.show()
    }

    private fun loadSelectedEpisode(target: PlaybackEpisode) {
        val anime = currentAnime ?: return
        val episode = episodeNavigator.select(target) ?: return
        loadEpisode(anime, episode)
    }

    private fun playNextEpisodeIfAvailable() {
        val anime = currentAnime ?: return
        if (isAutoNextRequested || !episodeNavigator.canMove(1)) return
        val episode = episodeNavigator.move(1) ?: return
        isAutoNextRequested = true
        episodePanel.dismiss(notifyCallbacks = false)
        loadEpisode(anime, episode, resetAutoNext = false)
    }

    private fun loadAdjacentEpisode(offset: Int) {
        val anime = currentAnime ?: return
        if (!transportVisible || controlsLocked || !episodeNavigator.canMove(offset)) return
        val episode = episodeNavigator.move(offset) ?: return
        loadEpisode(anime, episode)
    }

    private fun loadEpisode(
        anime: PlaybackAnime,
        episode: PlaybackEpisode,
        resetAutoNext: Boolean = true
    ) {
        requestedEpisode = episodeNavigator.selectedIndex + 1
        if (resetAutoNext) isAutoNextRequested = false
        updateEpisodeSelectionState()
        latestPlaybackState = PlaybackState()
        moreServersDisposable?.dispose()
        areMoreServersLoading = false
        hasAllServersFailed = false
        failedSourceIndexes.clear()
        resetSkipTimes()
        markSourcesLoading()
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

    private fun updateEpisodeSelectionState() {
        episodeRangeStart = PlayerEpisodePanelModel.rangeStart(
            episodes = episodeNavigator.episodes(),
            currentEpisode = episodeNavigator.currentEpisode
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
        fun offlineIntent(context: android.content.Context, entry: CompletedDownload): Intent {
            val episodeNumber = entry.episodeId.toIntOrNull() ?: 1
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ANILIST_ID, entry.aniListId)
                putExtra(EXTRA_TITLE, entry.title)
                putExtra(EXTRA_INITIAL_EPISODE, episodeNumber)
                putExtra(EXTRA_OFFLINE_FILE_PATH, entry.filePath)
                putExtra(EXTRA_OFFLINE_EPISODE_NAME, "Episode $episodeNumber")
            }
        }

        const val EXTRA_OFFLINE_FILE_PATH = "EXTRA_OFFLINE_FILE_PATH"
        const val EXTRA_OFFLINE_EPISODE_NAME = "EXTRA_OFFLINE_EPISODE_NAME"

        internal const val INVALID_ANILIST_ID = 0
        internal const val INVALID_MAL_ID = 0
        internal const val INVALID_TOTAL_EPISODES = 0
        internal const val DEFAULT_INITIAL_EPISODE = 1
        private const val INVALID_SOURCE_INDEX = -1
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        private const val PROGRESS_SAVE_INTERVAL_MS = 5_000L
        private const val MIN_RESUME_POSITION_MS = 1_000L
        internal const val CONTROLS_AUTO_HIDE_DELAY_MS = 4_000L
        private const val CUSTOM_SUBTITLE_FONT_DIRECTORY = "fonts"
        private const val CUSTOM_SUBTITLE_FONT_FILE_NAME = "custom_subtitle_font.ttf"
        private const val CUSTOM_SUBTITLE_FONT_TEMP_NAME = "custom_subtitle_font.tmp"
    }

    private class PlaybackUnavailableException : IllegalStateException()
}
