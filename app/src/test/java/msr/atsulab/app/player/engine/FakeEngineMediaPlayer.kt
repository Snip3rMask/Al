package msr.atsulab.app.player.engine

import msr.atsulab.app.player.domain.model.VideoSource

class FakeEngineMediaPlayer : EngineMediaPlayer {

    override var listener: PlaybackEngineListener? = null

    override var currentState: PlaybackState = PlaybackState()

    var preparedSources = mutableListOf<Pair<VideoSource, Long>>()
        private set

    var playCount = 0
        private set

    var pauseCount = 0
        private set

    var seekPositions = mutableListOf<Long>()
        private set

    var speeds = mutableListOf<Float>()
        private set

    var videoQualities = mutableListOf<String?>()
        private set

    var subtitleTracks = mutableListOf<String?>()
        private set

    var videoViews = mutableListOf<Any?>()
        private set

    var released = false
        private set

    override fun prepare(source: VideoSource, startPositionMs: Long) {
        preparedSources += source to startPositionMs
    }

    override fun play() {
        playCount++
        currentState = currentState.copy(isPlaying = true, playWhenReady = true)
    }

    override fun pause() {
        pauseCount++
        currentState = currentState.copy(isPlaying = false, playWhenReady = false)
    }

    override fun seekTo(positionMs: Long) {
        seekPositions += positionMs
    }

    override fun setSpeed(speed: Float) {
        speeds += speed
    }

    override fun setSubtitleTrack(trackId: String?) {
        subtitleTracks += trackId
    }

    override fun setVideoQuality(trackId: String?) {
        videoQualities += trackId
    }

    override fun setVideoView(videoView: Any?) {
        videoViews += videoView
    }

    override fun release() {
        released = true
    }
}
