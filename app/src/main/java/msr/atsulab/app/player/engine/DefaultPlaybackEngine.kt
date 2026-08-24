package msr.atsulab.app.player.engine

import msr.atsulab.app.player.domain.model.VideoSource

internal class DefaultPlaybackEngine(
    private val mediaPlayer: EngineMediaPlayer
) : PlaybackEngine {

    private var released = false
    private var resumeOnForeground = false

    init {
        mediaPlayer.listener = object : PlaybackEngineListener {
            override fun onStateChanged(state: PlaybackState) {
                listener?.onStateChanged(state)
            }

            override fun onError(error: PlaybackError) {
                listener?.onError(error)
            }
        }
    }

    override var listener: PlaybackEngineListener? = null

    override val currentState: PlaybackState
        get() = if (released) PlaybackState() else mediaPlayer.currentState

    override fun prepare(source: VideoSource, startPositionMs: Long) {
        if (released) return
        require(source.url.isNotBlank()) { "Playback URL must not be blank" }
        require(startPositionMs >= 0L) { "Start position must not be negative" }

        mediaPlayer.prepare(source, startPositionMs)
        emitState()
    }

    override fun play() {
        if (!released) {
            resumeOnForeground = false
            mediaPlayer.play()
            emitState()
        }
    }

    override fun pause() {
        if (!released) {
            resumeOnForeground = false
            mediaPlayer.pause()
            emitState()
        }
    }

    override fun seekTo(positionMs: Long) {
        if (released) return
        require(positionMs >= 0L) { "Seek position must not be negative" }
        mediaPlayer.seekTo(positionMs)
        emitState()
    }

    override fun setSpeed(speed: Float) {
        if (released) return
        require(speed > 0f) { "Speed must be greater than zero" }
        mediaPlayer.setSpeed(speed)
        emitState()
    }

    override fun setVideoView(videoView: Any?) {
        if (!released) mediaPlayer.setVideoView(videoView)
    }

    override fun onBackground() {
        if (released) return
        if (mediaPlayer.currentState.playWhenReady) {
            resumeOnForeground = true
            mediaPlayer.pause()
            emitState()
        }
    }

    override fun onForeground() {
        if (released || !resumeOnForeground) return
        resumeOnForeground = false
        mediaPlayer.play()
        emitState()
    }

    override fun release() {
        if (released) return
        released = true
        listener = null
        mediaPlayer.release()
    }

    private fun emitState() {
        listener?.onStateChanged(mediaPlayer.currentState)
    }
}
