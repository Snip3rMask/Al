package msr.atsulab.app.player.di

import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackHttpClientFactoryTest {

    @Test
    fun `playback http client preserves anifux timeouts and redirects`() {
        val client = PlaybackHttpClientFactory.create()

        assertEquals(20L, client.connectTimeoutMillis / TimeUnit.SECONDS.toMillis(1))
        assertEquals(35L, client.readTimeoutMillis / TimeUnit.SECONDS.toMillis(1))
        assertTrue(client.retryOnConnectionFailure)
        assertTrue(client.followRedirects)
        assertTrue(client.followSslRedirects)
    }

    @Test
    fun `factory creates independent clients`() {
        val firstClient = PlaybackHttpClientFactory.create()
        val secondClient = PlaybackHttpClientFactory.create()

        assertFalse(firstClient === secondClient)
    }
}
