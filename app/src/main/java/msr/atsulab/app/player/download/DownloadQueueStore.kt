package msr.atsulab.app.player.download

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class DownloadJobState {
    QUEUED,
    RUNNING,
    CANCELLING,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class DownloadJob(
    val id: String,
    val requests: List<DownloadRequest>,
    val currentIndex: Int = 0,
    val percent: Int = 0,
    val state: DownloadJobState = DownloadJobState.QUEUED,
    val error: String? = null
) {
    val currentRequest: DownloadRequest?
        get() = requests.getOrNull(currentIndex)

    init {
        require(requests.isNotEmpty()) { "Download job requires at least one request" }
    }
}

interface DownloadQueueStore {
    fun start(requests: List<DownloadRequest>): DownloadJob
    fun find(jobId: String): DownloadJob?
    fun begin(jobId: String, index: Int): Boolean
    fun updateProgress(jobId: String, index: Int, percent: Int)
    fun finish(jobId: String, index: Int)
    fun fail(jobId: String, message: String)
    fun requestCancel(jobId: String)
    fun confirmCancel(jobId: String)
    fun isCancelRequested(jobId: String): Boolean
    fun activeJobs(): List<DownloadJob>
}

class InMemoryDownloadQueueStore(
    private val newId: () -> String = { UUID.randomUUID().toString() }
) : DownloadQueueStore {

    private val jobs = ConcurrentHashMap<String, DownloadJob>()

    override fun start(requests: List<DownloadRequest>): DownloadJob {
        val job = DownloadJob(id = newId(), requests = requests)
        jobs[job.id] = job
        return job
    }

    override fun find(jobId: String): DownloadJob? = jobs[jobId]

    override fun begin(jobId: String, index: Int): Boolean {
        return mutate(jobId) { job ->
            when {
                job.state == DownloadJobState.CANCELLING || job.state == DownloadJobState.CANCELLED ->
                    job.copy(state = DownloadJobState.CANCELLED)
                index in job.requests.indices ->
                    job.copy(currentIndex = index, percent = 0, state = DownloadJobState.RUNNING, error = null)
                else -> job
            }
        }.state != DownloadJobState.CANCELLED
    }

    override fun updateProgress(jobId: String, index: Int, percent: Int) {
        mutate(jobId) { job ->
            if (job.currentIndex != index || job.state != DownloadJobState.RUNNING) {
                job
            } else {
                job.copy(percent = percent.coerceIn(0, 100))
            }
        }
    }

    override fun finish(jobId: String, index: Int) {
        mutate(jobId) { job ->
            if (job.currentIndex != index || job.state != DownloadJobState.RUNNING) {
                job
            } else if (index == job.requests.lastIndex) {
                job.copy(percent = 100, state = DownloadJobState.COMPLETED)
            } else {
                job
            }
        }
    }

    override fun fail(jobId: String, message: String) {
        mutate(jobId) { job ->
            if (job.state.isFinished) {
                job
            } else {
                job.copy(state = DownloadJobState.FAILED, error = message.ifBlank { "Download failed" })
            }
        }
    }

    override fun requestCancel(jobId: String) {
        mutate(jobId) { job ->
            when (job.state) {
                DownloadJobState.QUEUED, DownloadJobState.RUNNING ->
                    job.copy(state = DownloadJobState.CANCELLING)
                else -> job
            }
        }
    }

    override fun isCancelRequested(jobId: String): Boolean {
        return find(jobId)?.state == DownloadJobState.CANCELLING
    }

    override fun confirmCancel(jobId: String) {
        mutate(jobId) { job ->
            if (job.state == DownloadJobState.CANCELLING) {
                job.copy(state = DownloadJobState.CANCELLED)
            } else {
                job
            }
        }
    }

    override fun activeJobs(): List<DownloadJob> {
        return jobs.values.filterNot { it.state.isFinished }.sortedBy(DownloadJob::id)
    }

    private fun mutate(jobId: String, transform: (DownloadJob) -> DownloadJob): DownloadJob? {
        while (true) {
            val current = jobs[jobId] ?: return null
            val updated = transform(current)
            if (jobs.replace(jobId, current, updated)) return updated
        }
    }

    private val DownloadJobState.isFinished
        get() = this == DownloadJobState.COMPLETED || this == DownloadJobState.CANCELLED || this == DownloadJobState.FAILED
}
