package msr.atsulab.app.player.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import msr.atsulab.app.R
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import org.koin.android.ext.android.inject

class PlayerDownloadService : Service() {

    private val queueStore: DownloadQueueStore by inject()
    private val entryStore: DownloadEntryStore by inject()
    private val downloader: HlsDownloader by inject()
    private val preferences: PlaybackPreferencesStore by inject()
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        queueStore.recoverAfterProcessDeath()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getStringExtra(EXTRA_JOB_ID).orEmpty()
        var job = queueStore.find(jobId)
        if (job == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PAUSE -> {
                queueStore.requestPause(jobId)
                queueStore.find(jobId)?.let { pausingJob ->
                    updateNotification(jobId, getString(R.string.download_pausing), pausingJob.percent, false)
                }
                return START_NOT_STICKY
            }
            ACTION_CANCEL -> {
                queueStore.requestCancel(jobId)
                return START_NOT_STICKY
            }
            ACTION_RESUME -> queueStore.resume(jobId)
            ACTION_RETRY -> queueStore.retry(jobId)
            else -> Unit
        }

        job = queueStore.find(jobId)
        val activeJob = job ?: return START_NOT_STICKY.also { stopSelf(startId) }
        if (activeJob.state != DownloadJobState.QUEUED && activeJob.state != DownloadJobState.RUNNING) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startInForeground(activeJob, notification(activeJob))
        worker.execute {
            runDownload(startId, jobId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun runDownload(startId: Int, jobId: String) {
        try {
            while (true) {
                val job = queueStore.find(jobId) ?: return
                if (job.state.isTerminal) return
                val request = job.currentRequest ?: return
                val index = job.currentIndex
                if (!queueStore.begin(jobId, index)) return

                updateNotification(jobId, getString(R.string.download_running_format, request.displayName), 0, false)
                val file = downloader.download(
                    request = request,
                    parallelSegments = preferences.getDownloadParallelSegments(),
                    cancelToken = DownloadCancelToken { queueStore.isCancelRequested(jobId) },
                    pauseToken = { queueStore.isPauseRequested(jobId) },
                    sessionDirectory = sessionDirectory(jobId, request),
                    onProgress = DownloadProgressListener { doneSegments, totalSegments ->
                        val percent = progressPercent(doneSegments, totalSegments)
                        queueStore.updateProgress(jobId, index, percent)
                        updateNotification(jobId, request.displayName, percent, false)
                    }
                )

                if (queueStore.isCancelRequested(jobId)) {
                    file.delete()
                    return
                }
                entryStore.save(CompletedDownload.from(request, file, System.currentTimeMillis()))
                queueStore.finish(jobId, index)
                if (index == (queueStore.find(jobId)?.requests?.lastIndex ?: return)) return
            }
        } catch (_: DownloadPausedException) {
            queueStore.confirmPause(jobId)
        } catch (_: Exception) {
            if (queueStore.isCancelRequested(jobId)) {
                queueStore.confirmCancel(jobId)
            } else {
                queueStore.fail(jobId, "")
            }
        } finally {
            refreshFinalNotification(jobId)
            stopForeground(false)
            stopSelf(startId)
        }
    }

    private fun sessionDirectory(jobId: String, request: DownloadRequest): File {
        return File(downloader.sessionRoot(), "${jobId}-${request.sessionKey}")
    }

    private fun refreshFinalNotification(jobId: String) {
        val job = queueStore.find(jobId) ?: return
        val text = when (job.state) {
            DownloadJobState.PAUSED -> getString(R.string.download_paused)
            DownloadJobState.COMPLETED -> getString(R.string.download_completed)
            DownloadJobState.CANCELLED -> getString(R.string.download_cancelled)
            DownloadJobState.FAILED -> job.error ?: getString(R.string.download_failed)
            else -> return
        }
        updateNotification(jobId, text, job.percent, !job.state.isActive)
    }

    private fun updateNotification(jobId: String, text: String, percent: Int, done: Boolean) {
        val job = queueStore.find(jobId) ?: return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId(job), notification(job, text, percent, done))
    }

    private fun notification(job: DownloadJob): Notification {
        return notification(job, getString(R.string.download_queued), job.percent, false)
    }

    private fun notification(
        job: DownloadJob,
        text: String,
        percent: Int,
        done: Boolean
    ): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this,
            job.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.download_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setAutoCancel(done)

        if (!done) {
            builder.setOngoing(true).setProgress(100, percent.coerceIn(0, 100), false)
            builder.addAction(0, getString(R.string.pause), serviceAction(this, job.id, ACTION_PAUSE))
            builder.addAction(0, getString(R.string.cancel), serviceAction(this, job.id, ACTION_CANCEL))
        } else if (job.state == DownloadJobState.FAILED || job.state == DownloadJobState.CANCELLED) {
            builder.addAction(0, getString(R.string.retry), serviceAction(this, job.id, ACTION_RETRY))
        } else if (job.state == DownloadJobState.PAUSED) {
            builder.addAction(0, getString(R.string.resume), serviceAction(this, job.id, ACTION_RESUME))
        }
        return builder.build()
    }

    private fun startInForeground(job: DownloadJob, activeNotification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId(job),
                activeNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(notificationId(job), activeNotification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_notification_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.download_channel_description)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun progressPercent(doneSegments: Int, totalSegments: Int): Int {
        if (totalSegments <= 0) return 0
        return ((doneSegments.toDouble() / totalSegments) * 100).toInt().coerceIn(0, 100)
    }

    private fun notificationId(job: DownloadJob): Int {
        return NOTIFICATION_ID_BASE + ((job.id.hashCode() % 1000) + 1000) % 1000
    }

    private val DownloadJobState.isTerminal
        get() = this == DownloadJobState.COMPLETED ||
            this == DownloadJobState.CANCELLED ||
            this == DownloadJobState.FAILED

    companion object {
        private const val CHANNEL_ID = "atsu_player_downloads"
        private const val ACTION_START = "msr.atsulab.app.player.DOWNLOAD_START"
        private const val ACTION_PAUSE = "msr.atsulab.app.player.DOWNLOAD_PAUSE"
        private const val ACTION_RESUME = "msr.atsulab.app.player.DOWNLOAD_RESUME"
        private const val ACTION_CANCEL = "msr.atsulab.app.player.DOWNLOAD_CANCEL"
        private const val ACTION_RETRY = "msr.atsulab.app.player.DOWNLOAD_RETRY"
        private const val EXTRA_JOB_ID = "job_id"
        private const val NOTIFICATION_ID_BASE = 4200

        fun start(
            context: Context,
            queueStore: DownloadQueueStore,
            requests: List<DownloadRequest>
        ): String {
            val job = queueStore.start(requests)
            startJob(context, job.id)
            return job.id
        }

        fun startJob(context: Context, jobId: String) {
            ContextCompat.startForegroundService(context, serviceIntent(context, jobId, ACTION_START))
        }

        fun resume(context: Context, jobId: String) {
            ContextCompat.startForegroundService(context, serviceIntent(context, jobId, ACTION_RESUME))
        }

        fun pause(context: Context, jobId: String) {
            ContextCompat.startForegroundService(context, serviceIntent(context, jobId, ACTION_PAUSE))
        }

        fun retry(context: Context, jobId: String) {
            ContextCompat.startForegroundService(context, serviceIntent(context, jobId, ACTION_RETRY))
        }

        fun cancel(context: Context, jobId: String) {
            ContextCompat.startForegroundService(context, serviceIntent(context, jobId, ACTION_CANCEL))
        }

        private fun serviceIntent(context: Context, jobId: String, action: String): Intent {
            return Intent(context, PlayerDownloadService::class.java).apply {
                this.action = action
                putExtra(EXTRA_JOB_ID, jobId)
            }
        }

        private fun serviceAction(context: Context, jobId: String, action: String): PendingIntent {
            return PendingIntent.getService(
                context,
                jobId.hashCode() + action.hashCode(),
                serviceIntent(context, jobId, action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
