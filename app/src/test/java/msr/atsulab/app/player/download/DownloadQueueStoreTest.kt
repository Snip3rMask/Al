package msr.atsulab.app.player.download

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DownloadQueueStoreTest {

    private fun request(number: Int): DownloadRequest {
        return DownloadRequest(
            aniListId = 21,
            episodeId = number.toString(),
            displayName = "AtsuLab Anime",
            url = "https://example.com/episode-$number.m3u8"
        )
    }

    @Test
    fun `queue progresses through requests and completes final episode`() {
        val queue = InMemoryDownloadQueueStore(newId = { "job" })
        val job = queue.start(listOf(request(1), request(2)))

        assertTrue(queue.begin(job.id, 0))
        queue.updateProgress(job.id, 0, 45)
        assertEquals(DownloadJobState.RUNNING, queue.find(job.id)?.state)
        assertEquals(45, queue.find(job.id)?.percent)

        queue.finish(job.id, 0)
        assertTrue(queue.begin(job.id, 1))
        queue.finish(job.id, 1)

        val completed = queue.find(job.id)
        assertEquals(DownloadJobState.COMPLETED, completed?.state)
        assertEquals(100, completed?.percent)
        assertEquals(1, completed?.currentIndex)
    }

    @Test
    fun `pause request suspends current request and resume queues it again`() {
        val queue = InMemoryDownloadQueueStore(newId = { "job" })
        val job = queue.start(listOf(request(1)))
        assertTrue(queue.begin(job.id, 0))

        queue.requestPause(job.id)
        assertTrue(queue.isPauseRequested(job.id))
        assertFalse(queue.begin(job.id, 0))

        queue.confirmPause(job.id)
        assertEquals(DownloadJobState.PAUSED, queue.find(job.id)?.state)

        assertTrue(queue.resume(job.id))
        assertEquals(DownloadJobState.QUEUED, queue.find(job.id)?.state)
        assertTrue(queue.begin(job.id, 0))
    }

    @Test
    fun `failed job retries from failed episode index`() {
        val queue = InMemoryDownloadQueueStore(newId = { "job" })
        val job = queue.start(listOf(request(1), request(2)))
        assertTrue(queue.begin(job.id, 1))
        queue.fail(job.id, "network")

        val failed = queue.find(job.id)
        assertEquals(DownloadJobState.FAILED, failed?.state)
        assertEquals(1, failed?.currentIndex)

        assertTrue(queue.retry(job.id))
        val retried = queue.find(job.id)
        assertEquals(DownloadJobState.QUEUED, retried?.state)
        assertEquals(1, retried?.currentIndex)
    }

    @Test
    fun `running jobs recover as queued after process death`() {
        val queue = InMemoryDownloadQueueStore(newId = { "job" })
        val job = queue.start(listOf(request(1)))
        assertTrue(queue.begin(job.id, 0))

        queue.recoverAfterProcessDeath()

        assertEquals(DownloadJobState.QUEUED, queue.find(job.id)?.state)
    }

    @Test
    fun `cancel prevents queued work from starting`() {
        val queue = InMemoryDownloadQueueStore(newId = { "job" })
        val job = queue.start(listOf(request(1)))

        queue.requestCancel(job.id)
        assertTrue(queue.isCancelRequested(job.id))
        assertFalse(queue.begin(job.id, 0))

        queue.confirmCancel(job.id)
        assertEquals(DownloadJobState.CANCELLED, queue.find(job.id)?.state)
        assertTrue(queue.activeJobs().isEmpty())
    }

    @Test
    fun `paused job can be cancelled directly`() {
        val queue = InMemoryDownloadQueueStore(newId = { "job" })
        val job = queue.start(listOf(request(1)))
        assertTrue(queue.begin(job.id, 0))
        queue.requestPause(job.id)
        queue.confirmPause(job.id)

        queue.requestCancel(job.id)
        queue.confirmCancel(job.id)

        assertEquals(DownloadJobState.CANCELLED, queue.find(job.id)?.state)
    }
}
