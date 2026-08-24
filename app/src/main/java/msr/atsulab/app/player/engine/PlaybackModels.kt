package msr.atsulab.app.player.engine

import msr.atsulab.app.player.domain.model.VideoSource

enum class PlaybackReadyState {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

enum class PlaybackErrorType {
    NETWORK,
    CONTENT,
    DECODING,
    AUDIO_TRACK,
    DRM,
    UNKNOWN
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val readyState: PlaybackReadyState = PlaybackReadyState.IDLE,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f
)

data class PlaybackError(
    val type: PlaybackErrorType,
    val message: String,
    val cause: Throwable? = null
)

interface PlaybackEngineListener {
    fun onStateChanged(state: PlaybackState)

    fun onError(error: PlaybackError)
}

interface PlaybackEngine {
    var listener: PlaybackEngineListener?

    fun prepare(source: VideoSource, startPositionMs: Long = 0L)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    fun setVideoView(videoView: Any?)

    fun onBackground()

    fun onForeground()

    fun release()
}

internal interface EngineMediaPlayer {
    var listener: PlaybackEngineListener?

    val currentState: PlaybackState

    fun prepare(source: VideoSource, startPositionMs: Long)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    fun setVideoView(videoView: Any?)

    fun release()
}
