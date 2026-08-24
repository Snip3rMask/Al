package msr.atsulab.app.player.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import msr.atsulab.app.R
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.engine.PlaybackError
import msr.atsulab.app.player.domain.repository.EpisodeRepository
import msr.atsulab.app.player.domain.repository.VideoSourceRepository
import msr.atsulab.app.player.engine.PlaybackEngine
import msr.atsulab.app.player.engine.PlaybackEngineListener
import msr.atsulab.app.player.engine.PlaybackReadyState
import msr.atsulab.app.player.engine.PlaybackState
import msr.atsulab.app.player.runtime.selectPlaybackEpisode
import org.koin.java.KoinJavaComponent.inject

class PlayerActivity : AppCompatActivity() {

    private val episodeRepository: EpisodeRepository by inject(EpisodeRepository::class.java)
    private val videoSourceRepository: VideoSourceRepository by inject(VideoSourceRepository::class.java)
    private val playbackEngine: PlaybackEngine by inject(PlaybackEngine::class.java)

    private lateinit var statusView: TextView
    private lateinit var retryButton: Button
    private lateinit var surfaceView: SurfaceView

    private var currentAnime: PlaybackAnime? = null
    private var requestedEpisode = DEFAULT_INITIAL_EPISODE
    private var engineAttached = false
    private var playbackDisposable: Disposable? = null

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

        setupContent()
        attachEngine(playbackEngine)
        engineAttached = true
        loadPlayback()
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

    private fun attachEngine(engine: PlaybackEngine) {
        engine.setSurfaceView(surfaceView)
        engine.listener = object : PlaybackEngineListener {
            override fun onStateChanged(state: PlaybackState) {
                when (state.readyState) {
                    PlaybackReadyState.BUFFERING -> showMessage(R.string.player_buffering)
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

    private fun setupContent() {
        surfaceView = SurfaceView(this)
        val surfaceHost = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        surfaceHost.addView(
            surfaceView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        statusView = TextView(this).apply {
            gravity = Gravity.CENTER
            setPadding(48, 24, 48, 8)
            setTextColor(Color.WHITE)
        }

        retryButton = MaterialButton(this).apply {
            setText(R.string.retry)
            visibility = View.GONE
            setOnClickListener {
                val anime = currentAnime ?: return@setOnClickListener
                loadPlayback(anime)
            }
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            addView(statusView)
            addView(retryButton)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            addView(
                surfaceHost,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            addView(
                controls,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        setContentView(root)
    }

    private fun loadPlayback() {
        val anime = currentAnime ?: return
        loadPlayback(anime)
    }

    private fun loadPlayback(anime: PlaybackAnime) {
        showMessage(R.string.player_loading_episode, anime.title, requestedEpisode)
        retryButton.visibility = View.GONE
        playbackDisposable?.dispose()

        playbackDisposable = episodeRepository.getEpisodes(anime)
            .flatMap { episodes ->
                val selectedEpisode = episodes.selectPlaybackEpisode(requestedEpisode)
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

        showMessage(R.string.player_starting_playback, episode.name)
        playbackEngine.prepare(source)
        playbackEngine.play()
    }

    private fun showMessage(messageResId: Int, vararg args: Any?) {
        statusView.text = getString(messageResId, *args)
        retryButton.visibility = View.GONE
    }

    private fun showUnavailable() {
        statusView.text = getString(R.string.playback_unavailable)
        retryButton.visibility = View.VISIBLE
    }

    private fun showError() {
        statusView.text = getString(R.string.player_runtime_error)
        retryButton.visibility = View.VISIBLE
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
