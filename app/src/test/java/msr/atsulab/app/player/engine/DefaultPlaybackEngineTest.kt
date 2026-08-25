package msr.atsulab.app.player.engine

import msr.atsulab.app.player.domain.model.VideoSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultPlaybackEngineTest {

    private val source = VideoSource(quality = "1080p", url = "https://example.test/video.m3u8")

    @Test
    fun `prepare validates input and delegates to media player`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val states = mutableListOf<PlaybackState>()
        val engine = DefaultPlaybackEngine(mediaPlayer)
        engine.listener = object : PlaybackEngineListener {
            override fun onStateChanged(state: PlaybackState) {
                states += state
            }

            override fun onError(error: PlaybackError) = Unit
        }

        engine.prepare(source, startPositionMs = 1_250L)

        assertEquals(listOf(source to 1_250L), mediaPlayer.preparedSources)
        assertEquals(1, states.size)
        assertThrows(IllegalArgumentException::class.java) { engine.prepare(source.copy(url = " ")) }
        assertThrows(IllegalArgumentException::class.java) { engine.prepare(source, -1L) }
    }

    @Test
    fun `background pauses active playback and foreground resumes once`() {
        val mediaPlayer = FakeEngineMediaPlayer().apply {
            currentState = PlaybackState(isPlaying = true, playWhenReady = true)
        }
        val engine = DefaultPlaybackEngine(mediaPlayer)

        engine.onBackground()
        engine.onForeground()
        engine.onForeground()

        assertEquals(1, mediaPlayer.pauseCount)
        assertEquals(1, mediaPlayer.playCount)
    }

    @Test
    fun `paused playback does not resume after foreground`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val engine = DefaultPlaybackEngine(mediaPlayer)

        engine.onBackground()
        engine.onForeground()

        assertEquals(0, mediaPlayer.playCount)
    }

    @Test
    fun `transport controls validate and delegate commands`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val engine = DefaultPlaybackEngine(mediaPlayer)

        engine.seekTo(2_500L)
        engine.setSpeed(1.25f)

        assertEquals(listOf(2_500L), mediaPlayer.seekPositions)
        assertEquals(listOf(1.25f), mediaPlayer.speeds)
        assertThrows(IllegalArgumentException::class.java) { engine.seekTo(-1L) }
        assertThrows(IllegalArgumentException::class.java) { engine.setSpeed(0f) }
    }

    @Test
    fun `subtitle selection delegates without repreparing source`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val engine = DefaultPlaybackEngine(mediaPlayer)

        engine.prepare(source)
        engine.setSubtitleTrack("1:2")
        engine.setSubtitleTrack(EXTERNAL_SUBTITLE_TRACK_ID)
        engine.setSubtitleTrack(null)

        assertEquals(1, mediaPlayer.preparedSources.size)
        assertEquals(listOf("1:2", EXTERNAL_SUBTITLE_TRACK_ID, null), mediaPlayer.subtitleTracks)
    }

    @Test
    fun `speed change does not reprepare or reset playback position`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val engine = DefaultPlaybackEngine(mediaPlayer)

        engine.prepare(source)
        engine.setSpeed(1.5f)

        assertEquals(1, mediaPlayer.preparedSources.size)
        assertEquals(0, mediaPlayer.seekPositions.size)
        assertEquals(listOf(1.5f), mediaPlayer.speeds)
    }

    @Test
    fun `current state delegates before release and resets after release`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val playingState = PlaybackState(isPlaying = true, playWhenReady = true)
        mediaPlayer.currentState = playingState
        val engine = DefaultPlaybackEngine(mediaPlayer)

        assertEquals(playingState, engine.currentState)

        engine.release()

        assertEquals(PlaybackState(), engine.currentState)
    }

    @Test
    fun `release is idempotent and blocks later commands`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val engine = DefaultPlaybackEngine(mediaPlayer)

        engine.release()
        engine.release()
        engine.play()
        engine.prepare(source)

        assertTrue(mediaPlayer.released)

        engine.setSubtitleTrack("1:2")

        assertTrue(mediaPlayer.released)
        assertEquals(0, mediaPlayer.playCount)
        assertEquals(0, mediaPlayer.preparedSources.size)
        assertTrue(mediaPlayer.subtitleTracks.isEmpty())
    }

    @Test
    fun `video view attachment is delegated before release`() {
        val mediaPlayer = FakeEngineMediaPlayer()
        val engine = DefaultPlaybackEngine(mediaPlayer)
        val videoView = Any()

        engine.setVideoView(videoView)
        engine.release()
        engine.setVideoView(videoView)

        assertEquals(listOf<Any?>(videoView), mediaPlayer.videoViews)
        assertFalse(mediaPlayer.videoViews.size == 2)
    }
}
