package msr.atsulab.app.player.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.ui.PlayerView
import msr.atsulab.app.player.domain.model.SubtitleTrack
import msr.atsulab.app.player.domain.model.VideoQuality
import msr.atsulab.app.player.domain.model.VideoSource

internal class Media3EngineMediaPlayer(
    private val context: Context,
    private val errorMapper: PlaybackErrorMapper = PlaybackErrorMapper()
) : EngineMediaPlayer {

    private var playerView: PlayerView? = null
    private val trackSelector = DefaultTrackSelector(context.applicationContext)

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
                speed = player.playbackParameters.speed,
                subtitleTracks = readSubtitleTracks(),
                videoQualities = readVideoQualities()
            )
        }

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setTrackSelector(trackSelector)
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

            override fun onTracksChanged(tracks: Tracks) {
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
                .setLabel("English")
                .setLanguage("en")
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

    override fun setSubtitleTrack(trackId: String?) {
        val parametersBuilder = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, trackId == null)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)

        if (trackId != null) {
            if (trackId == EXTERNAL_SUBTITLE_TRACK_ID) {
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                )
                return
            }
            val (groupIndex, trackIndex) = parseTrackId(trackId)
            val group = player.currentTracks.groups.getOrNull(groupIndex)
            if (group == null || group.type != C.TRACK_TYPE_TEXT || trackIndex >= group.length) {
                return
            }
            parametersBuilder.setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            )
        }

        trackSelector.setParameters(parametersBuilder)
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

    private fun readSubtitleTracks(): List<SubtitleTrack> {
        return player.currentTracks.groups.flatMapIndexed { groupIndex, group ->
            (0 until group.length).mapNotNull { trackIndex ->
                if (group.type != C.TRACK_TYPE_TEXT) return@mapNotNull null
                val format = group.getTrackFormat(trackIndex)
                val label = SubtitleTrackMetadata.displayLabel(format.label, format.language)
                    ?: return@mapNotNull null

                SubtitleTrack(
                    id = "$groupIndex:$trackIndex",
                    label = label,
                    language = format.language.orEmpty(),
                    isSelected = group.isTrackSelected(trackIndex)
                )
            }
        }
    }

    private fun readVideoQualities(): List<VideoQuality> {
        return VideoQualityMetadata.sorted(
            player.currentTracks.groups.flatMapIndexed { groupIndex, group ->
                (0 until group.length).mapNotNull { trackIndex ->
                    if (group.type != C.TRACK_TYPE_VIDEO) return@mapNotNull null
                    val format = group.getTrackFormat(trackIndex)
                    VideoQualityMetadata.create(
                        id = "$groupIndex:$trackIndex",
                        fallbackLabel = format.label.orEmpty(),
                        width = format.width,
                        height = format.height,
                        bitrate = format.bitrate.toLong(),
                        isSelected = group.isTrackSelected(trackIndex)
                    )
                }
            }
        )
    }

    private fun parseTrackId(trackId: String): Pair<Int, Int> {
        val indexes = trackId.split(":")
        val groupIndex = indexes.getOrNull(0)?.toIntOrNull() ?: return -1 to -1
        val trackIndex = indexes.getOrNull(1)?.toIntOrNull() ?: return -1 to -1
        return groupIndex to trackIndex
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
