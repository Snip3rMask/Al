package msr.atsulab.app.player.engine

import androidx.media3.common.PlaybackException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaybackErrorMapperTest {

    private val mapper = PlaybackErrorMapper()

    @Test
    fun `network failures map to network category`() {
        val error = mapper.map(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            "timeout",
            null
        )

        assertEquals(PlaybackErrorType.NETWORK, error.type)
        assertEquals("timeout", error.message)
    }

    @Test
    fun `malformed media maps to content category`() {
        val error = mapper.map(
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            "bad container",
            IllegalStateException()
        )

        assertEquals(PlaybackErrorType.CONTENT, error.type)
        assertEquals("bad container", error.message)
    }

    @Test
    fun `codec audio drm and unknown failures use dedicated categories`() {
        assertEquals(
            PlaybackErrorType.DECODING,
            mapper.map(PlaybackException.ERROR_CODE_DECODING_FAILED, "", null).type
        )
        assertEquals(
            PlaybackErrorType.AUDIO_TRACK,
            mapper.map(PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED, "", null).type
        )
        assertEquals(
            PlaybackErrorType.DRM,
            mapper.map(PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED, "", null).type
        )
        assertEquals(
            PlaybackErrorType.UNKNOWN,
            mapper.map(-999, "", null).type
        )
    }
}
