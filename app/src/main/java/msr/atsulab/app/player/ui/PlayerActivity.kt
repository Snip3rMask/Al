package msr.atsulab.app.player.ui

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import msr.atsulab.app.R
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
import org.koin.java.KoinJavaComponent.inject

class PlayerActivity : AppCompatActivity() {

    private val episodeRepository: EpisodeRepository by inject(EpisodeRepository::class.java)
    private val videoSourceRepository: VideoSourceRepository by inject(VideoSourceRepository::class.java)
    private val playbackEngine: PlaybackEngine by inject(PlaybackEngine::class.java)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aniListId = intent.getIntExtra(EXTRA_ANILIST_ID, INVALID_ANILIST_ID)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        requestedEpisode = intent.getIntExtra(EXTRA_INITIAL_EPISODE, DEFAULT_INITIAL_EPISODE)
            .coerceAtLeast(DEFAULT_INITIAL_EPISODE)

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
    }

    override fun onStop() {
        if (engineAttached) playbackEngine.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
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
                when (state.readyState) {
                    PlaybackReadyState.BUFFERING -> showMessage(R.string.player_buffering, loading = true)
                    PlaybackReadyState.READY -> showMessage(R.string.player_playing)
                    PlaybackReadyState.ENDED -> showMessage(R.string.player_ended)
                    PlaybackReadyState.IDLE -> Unit
                }
            }

            override fun onError(error: PlaybackError) {
                showError()
            }
        }
    }

    private fun rebuildShell() {
        val callbacks = object : PlayerControllerSkeleton.Callbacks {
            override fun onBackClicked() {
                finish()
            }

            override fun onRetryClicked() {
                loadPlayback()
            }

            override fun onPlayPauseClicked() = Unit

            override fun onPreviousEpisodeClicked() = Unit

            override fun onNextEpisodeClicked() = Unit
        }
        val title = currentAnime?.title.orEmpty()
        val episodeLabel = getString(R.string.player_shell_status_format, title, requestedEpisode)

        shell = PlayerShellLayoutBuilder(this, callbacks).build(title, episodeLabel)
        playerView = shell.playerView
        loadingIndicator = shell.loadingIndicator
        setContentView(shell.root)
        applySystemBars()
        shell.controller.setStatus(statusMessage, statusRetryVisible)
        loadingIndicator.visibility = if (waitingForPlayback) View.VISIBLE else View.GONE
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

        showMessage(R.string.player_starting_playback, arguments = listOf(episode.name), loading = true)
        playbackEngine.prepare(source)
        playbackEngine.play()
    }

    private fun loadAdjacentEpisode(offset: Int) {
        val anime = currentAnime ?: return
        val episode = episodeNavigator.move(offset) ?: return

        requestedEpisode = episodeNavigator.selectedIndex + 1
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
        showMessage(R.string.playback_unavailable, retryVisible = true)
    }

    private fun showError() {
        showMessage(R.string.player_runtime_error, retryVisible = true)
    }

    private fun updateStatus(message: String, retryVisible: Boolean, loading: Boolean) {
        statusMessage = message
        statusRetryVisible = retryVisible
        waitingForPlayback = loading
        shell.controller.setStatus(statusMessage, statusRetryVisible)
        loadingIndicator.visibility = if (waitingForPlayback) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_ANILIST_ID = "EXTRA_ANILIST_ID"
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
    }

    private class PlaybackUnavailableException : IllegalStateException()
}
