package msr.atsulab.app.player.download

import android.content.Context
import com.google.gson.Gson
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class DownloadJobState {
    QUEUED,
    RUNNING,
    PAUSE_REQUESTED,
    PAUSED,
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
    fun allJobs(): List<DownloadJob>
    fun begin(jobId: String, index: Int): Boolean
    fun updateProgress(jobId: String, index: Int, percent: Int)
    fun finish(jobId: String, index: Int)
    fun fail(jobId: String, message: String)
    fun requestPause(jobId: String)
    fun requestCancel(jobId: String)
    fun isPauseRequested(jobId: String): Boolean
    fun isCancelRequested(jobId: String): Boolean
    fun confirmPause(jobId: String)
    fun confirmCancel(jobId: String)
    fun resume(jobId: String): Boolean
    fun retry(jobId: String): Boolean
    fun remove(jobId: String)
    fun activeJobs(): List<DownloadJob>
    fun recoverAfterProcessDeath()
}

class DefaultDownloadQueueStore(
    context: Context,
    private val gson: Gson = Gson(),
    private val newId: () -> String = { UUID.randomUUID().toString() }
) : DownloadQueueStore {

    private val lock = Any()
    private val preferences by lazy {
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun start(requests: List<DownloadRequest>): DownloadJob {
        synchronized(lock) {
            val duplicate = allJobsLocked().firstOrNull { job ->
                job.requests.map { it.sessionKey } == requests.map { it.sessionKey } &&
                    !job.state.isTerminalExceptCancelled
            }
            if (duplicate != null) return duplicate

            val job = DownloadJob(id = newId(), requests = requests)
            saveAllLocked(allJobsLocked() + job)
            return job
        }
    }

    override fun find(jobId: String): DownloadJob? = synchronized(lock) {
        allJobsLocked().firstOrNull { it.id == jobId }
    }

    override fun allJobs(): List<DownloadJob> = synchronized(lock) {
        allJobsLocked().sortedWith(compareByDescending<DownloadJob> { it.state.isActive }.thenBy(DownloadJob::id))
    }

    override fun begin(jobId: String, index: Int): Boolean = mutate(jobId) { job ->
        when {
            job.state == DownloadJobState.PAUSE_REQUESTED || job.state == DownloadJobState.PAUSED ->
                job.copy(state = DownloadJobState.PAUSED)
            job.state == DownloadJobState.CANCELLING || job.state == DownloadJobState.CANCELLED ->
                job.copy(state = DownloadJobState.CANCELLED)
            index in job.requests.indices ->
                job.copy(currentIndex = index, percent = 0, state = DownloadJobState.RUNNING, error = null)
            else -> job
        }
    }?.state == DownloadJobState.RUNNING

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

    override fun requestPause(jobId: String) {
        mutate(jobId) { job ->
            when (job.state) {
                DownloadJobState.QUEUED -> job.copy(state = DownloadJobState.PAUSED)
                DownloadJobState.RUNNING -> job.copy(state = DownloadJobState.PAUSE_REQUESTED)
                else -> job
            }
        }
    }

    override fun requestCancel(jobId: String) {
        mutate(jobId) { job ->
            when (job.state) {
                DownloadJobState.QUEUED -> job.copy(state = DownloadJobState.CANCELLED)
                DownloadJobState.RUNNING, DownloadJobState.PAUSE_REQUESTED, DownloadJobState.PAUSED ->
                    job.copy(state = DownloadJobState.CANCELLING)
                else -> job
            }
        }
    }

    override fun isPauseRequested(jobId: String): Boolean {
        return find(jobId)?.state == DownloadJobState.PAUSE_REQUESTED
    }

    override fun isCancelRequested(jobId: String): Boolean {
        return find(jobId)?.state == DownloadJobState.CANCELLING
    }

    override fun confirmPause(jobId: String) {
        mutate(jobId) { job ->
            if (job.state == DownloadJobState.PAUSE_REQUESTED) {
                job.copy(state = DownloadJobState.PAUSED)
            } else {
                job
            }
        }
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

    override fun resume(jobId: String): Boolean {
        val resumed = mutate(jobId) { job ->
            if (job.state == DownloadJobState.PAUSED) {
                job.copy(state = DownloadJobState.QUEUED, error = null)
            } else {
                job
            }
        }
        return resumed?.state == DownloadJobState.QUEUED
    }

    override fun retry(jobId: String): Boolean {
        val retried = mutate(jobId) { job ->
            if (job.state == DownloadJobState.FAILED || job.state == DownloadJobState.CANCELLED) {
                job.copy(
                    currentIndex = job.currentIndex.coerceIn(0, job.requests.lastIndex),
                    percent = 0,
                    state = DownloadJobState.QUEUED,
                    error = null
                )
            } else {
                job
            }
        }
        return retried?.state == DownloadJobState.QUEUED
    }

    override fun remove(jobId: String) {
        synchronized(lock) {
            saveAllLocked(allJobsLocked().filterNot { it.id == jobId })
        }
    }

    override fun activeJobs(): List<DownloadJob> {
        return allJobs().filter { it.state.isActive || it.state == DownloadJobState.FAILED }
    }

    override fun recoverAfterProcessDeath() {
        mutateAll { jobs ->
            jobs.map { job ->
                if (job.state == DownloadJobState.RUNNING || job.state == DownloadJobState.PAUSE_REQUESTED) {
                    job.copy(state = DownloadJobState.QUEUED)
                } else {
                    job
                }
            }
        }
    }

    private fun allJobsLocked(): List<DownloadJob> {
        val raw = preferences.getString(JOBS_KEY, null) ?: return emptyList()
        return try {
            gson.fromJson(raw, Array<DownloadJob>::class.java).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveAllLocked(jobs: List<DownloadJob>) {
        preferences.edit().putString(JOBS_KEY, gson.toJson(jobs)).apply()
    }

    private fun mutate(jobId: String, transform: (DownloadJob) -> DownloadJob): DownloadJob? {
        synchronized(lock) {
            val jobs = allJobsLocked()
            val index = jobs.indexOfFirst { it.id == jobId }
            if (index < 0) return null
            val updated = transform(jobs[index])
            val newJobs = jobs.toMutableList()
            newJobs[index] = updated
            saveAllLocked(newJobs)
            return updated
        }
    }

    private fun mutateAll(transform: (List<DownloadJob>) -> List<DownloadJob>) {
        synchronized(lock) {
            saveAllLocked(transform(allJobsLocked()))
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "atsu_download_queue"
        const val JOBS_KEY = "jobs"
    }
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

    override fun allJobs(): List<DownloadJob> {
        return jobs.values.sortedWith(compareByDescending<DownloadJob> { it.state.isActive }.thenBy(DownloadJob::id))
    }

    override fun begin(jobId: String, index: Int): Boolean {
        return mutate(jobId) { job ->
            when {
                job.state == DownloadJobState.PAUSE_REQUESTED || job.state == DownloadJobState.PAUSED ->
                    job.copy(state = DownloadJobState.PAUSED)
                job.state == DownloadJobState.CANCELLING || job.state == DownloadJobState.CANCELLED ->
                    job.copy(state = DownloadJobState.CANCELLED)
                index in job.requests.indices ->
                    job.copy(currentIndex = index, percent = 0, state = DownloadJobState.RUNNING, error = null)
                else -> job
            }
        }?.state == DownloadJobState.RUNNING
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

    override fun requestPause(jobId: String) {
        mutate(jobId) { job ->
            when (job.state) {
                DownloadJobState.QUEUED -> job.copy(state = DownloadJobState.PAUSED)
                DownloadJobState.RUNNING -> job.copy(state = DownloadJobState.PAUSE_REQUESTED)
                else -> job
            }
        }
    }

    override fun requestCancel(jobId: String) {
        mutate(jobId) { job ->
            when (job.state) {
                DownloadJobState.QUEUED -> job.copy(state = DownloadJobState.CANCELLED)
                DownloadJobState.RUNNING, DownloadJobState.PAUSE_REQUESTED, DownloadJobState.PAUSED ->
                    job.copy(state = DownloadJobState.CANCELLING)
                else -> job
            }
        }
    }

    override fun isPauseRequested(jobId: String): Boolean {
        return find(jobId)?.state == DownloadJobState.PAUSE_REQUESTED
    }

    override fun isCancelRequested(jobId: String): Boolean {
        return find(jobId)?.state == DownloadJobState.CANCELLING
    }

    override fun confirmPause(jobId: String) {
        mutate(jobId) { job ->
            if (job.state == DownloadJobState.PAUSE_REQUESTED) {
                job.copy(state = DownloadJobState.PAUSED)
            } else {
                job
            }
        }
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

    override fun resume(jobId: String): Boolean {
        val resumed = mutate(jobId) { job ->
            if (job.state == DownloadJobState.PAUSED) {
                job.copy(state = DownloadJobState.QUEUED, error = null)
            } else {
                job
            }
        }
        return resumed?.state == DownloadJobState.QUEUED
    }

    override fun retry(jobId: String): Boolean {
        val retried = mutate(jobId) { job ->
            if (job.state == DownloadJobState.FAILED || job.state == DownloadJobState.CANCELLED) {
                job.copy(
                    currentIndex = job.currentIndex.coerceIn(0, job.requests.lastIndex),
                    percent = 0,
                    state = DownloadJobState.QUEUED,
                    error = null
                )
            } else {
                job
            }
        }
        return retried?.state == DownloadJobState.QUEUED
    }

    override fun remove(jobId: String) {
        jobs.remove(jobId)
    }

    override fun activeJobs(): List<DownloadJob> {
        return allJobs().filter { it.state.isActive || it.state == DownloadJobState.FAILED }
    }

    override fun recoverAfterProcessDeath() {
        jobs.replaceAll { _, job ->
            if (job.state == DownloadJobState.RUNNING || job.state == DownloadJobState.PAUSE_REQUESTED) {
                job.copy(state = DownloadJobState.QUEUED)
            } else {
                job
            }
        }
    }

    private fun mutate(jobId: String, transform: (DownloadJob) -> DownloadJob): DownloadJob? {
        while (true) {
            val current = jobs[jobId] ?: return null
            val updated = transform(current)
            if (jobs.replace(jobId, current, updated)) return updated
        }
    }
}


private val DownloadJobState.isActive
    get() = this == DownloadJobState.QUEUED ||
        this == DownloadJobState.RUNNING ||
        this == DownloadJobState.PAUSE_REQUESTED ||
        this == DownloadJobState.CANCELLING

private val DownloadJobState.isFinished
    get() = this == DownloadJobState.COMPLETED ||
        this == DownloadJobState.CANCELLED ||
        this == DownloadJobState.FAILED

private val DownloadJobState.isTerminalExceptCancelled
    get() = this == DownloadJobState.COMPLETED || this == DownloadJobState.FAILED
