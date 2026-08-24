package msr.atsulab.app.player.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.ui.PlayerView
import msr.atsulab.app.player.domain.model.VideoSource

internal class Media3EngineMediaPlayer(
    private val context: Context,
    private val errorMapper: PlaybackErrorMapper = PlaybackErrorMapper()
) : EngineMediaPlayer {

    private var playerView: PlayerView? = null

    override var listener: PlaybackEngineListener? = null

    override val currentState: PlaybackState
        get() {
            val playbackState = when (player.playbackState) {
                Player.STATE_BUFFERING -> PlaybackReadyState.BUFFERING
                Player.STATE_READY -> PlaybackReadyState.READY
                Player.STATE_ENDED -> PlaybackReadyState.ENDED
                else -> PlaybackReadyState.IDLE
            }
            val durationMs = if (player.duration == C.TIME_UNSET) 0L else player.duration.coerceAtLeast(0L)

            return PlaybackState(
                isPlaying = player.isPlaying,
                playWhenReady = player.playWhenReady,
                readyState = playbackState,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                durationMs = durationMs,
                speed = player.playbackParameters.speed
            )
        }

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                emitState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                emitState()
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                emitState()
            }

            override fun onPlayerError(error: PlaybackException) {
                listener?.onError(errorMapper.map(error.errorCode, error.message ?: "Playback failed", error.cause))
                emitState()
            }
        })
    }

    override fun prepare(source: VideoSource, startPositionMs: Long) {
        val dataSourceFactory = createDataSourceFactory(source)
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(source.url))

        val subtitleUrl = source.subtitleUrl
        val mergedSource = if (subtitleUrl.isBlank()) {
            mediaSource
        } else {
            val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                .setMimeType(SubtitleMimeTypes.fromUrl(subtitleUrl))
                .setLanguage("und")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            val subtitleSource = SingleSampleMediaSource.Factory(dataSourceFactory)
                .createMediaSource(subtitleConfiguration, C.TIME_UNSET)
            MergingMediaSource(mediaSource, subtitleSource)
        }

        player.setMediaSource(mergedSource, startPositionMs)
        player.prepare()
        player.playWhenReady = true
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    override fun setVideoView(videoView: Any?) {
        require(videoView is PlayerView) { "A Media3 PlayerView is required" }
        playerView?.player = null
        playerView = videoView
        videoView.player = player
    }

    override fun release() {
        listener = null
        playerView?.player = null
        playerView = null
        player.release()
    }

    private fun emitState() {
        listener?.onStateChanged(currentState)
    }

    private fun createDataSourceFactory(source: VideoSource): DefaultDataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)

        if (source.referer.isNotBlank()) {
            httpFactory.setDefaultRequestProperties(mapOf("Referer" to source.referer))
        }

        return DefaultDataSource.Factory(context.applicationContext, httpFactory)
    }

    private companion object {
        const val USER_AGENT = "AtsuLab/2.1 (Android)"
        const val CONNECT_TIMEOUT_MS = 20_000
        const val READ_TIMEOUT_MS = 35_000
    }
}
