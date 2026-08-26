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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import msr.atsulab.app.R
import org.koin.android.ext.android.inject

class PlayerDownloadService : Service() {

    private val queueStore: DownloadQueueStore by inject()
    private val entryStore: DownloadEntryStore by inject()
    private val downloader: HlsDownloader by inject()
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL && intent.hasExtra(EXTRA_JOB_ID)) {
            queueStore.requestCancel(intent.getStringExtra(EXTRA_JOB_ID).orEmpty())
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START || !intent.hasExtra(EXTRA_JOB_ID)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val jobId = intent.getStringExtra(EXTRA_JOB_ID).orEmpty()
        val job = queueStore.find(jobId)
        if (job == null || job.state.isTerminal) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startInForeground(job, notification(job, getString(R.string.download_queued), 0, done = false))
        worker.execute {
            runDownload(startId, job.id)
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
            var index = queueStore.find(jobId)?.currentIndex ?: return
            while (true) {
                val job = queueStore.find(jobId) ?: return
                val request = job.currentRequest ?: break
                if (job.state.isTerminal) break
                if (!queueStore.begin(jobId, index)) break

                updateNotification(
                    jobId,
                    getString(R.string.download_running_format, request.displayName),
                    0,
                    false
                )
                var lastPercent = 0
                val file = downloader.download(
                    request,
                    cancelToken = DownloadCancelToken { queueStore.isCancelRequested(jobId) },
                    onProgress = DownloadProgressListener { doneSegments, totalSegments ->
                        lastPercent = progressPercent(doneSegments, totalSegments)
                        queueStore.updateProgress(jobId, index, lastPercent)
                        updateNotification(jobId, request.displayName, lastPercent, false)
                    }
                )
                if (queueStore.isCancelRequested(jobId)) {
                    file.delete()
                    break
                }
                entryStore.save(CompletedDownload.from(request, file, System.currentTimeMillis()))
                queueStore.finish(jobId, index)
                if (index == job.requests.lastIndex) break
                index += 1
            }

            val finalJob = queueStore.find(jobId)
            when (finalJob?.state) {
                DownloadJobState.CANCELLING -> {
                    queueStore.confirmCancel(jobId)
                    updateNotification(jobId, getString(R.string.download_cancelled), finalJob.percent, true)
                }
                DownloadJobState.COMPLETED -> updateNotification(jobId, getString(R.string.download_completed), 100, true)
                DownloadJobState.FAILED -> updateNotification(
                    jobId,
                    finalJob.error ?: getString(R.string.download_failed),
                    finalJob.percent,
                    true
                )
                else -> Unit
            }
        } catch (_: Exception) {
            val cancelled = queueStore.isCancelRequested(jobId)
            if (cancelled) {
                queueStore.confirmCancel(jobId)
                updateNotification(jobId, getString(R.string.download_cancelled), queueStore.find(jobId)?.percent ?: 0, true)
            } else {
                queueStore.fail(jobId, "")
                updateNotification(jobId, getString(R.string.download_failed), queueStore.find(jobId)?.percent ?: 0, true)
            }
        } finally {
            stopForeground(false)
            stopSelf(startId)
        }
    }


    private fun updateNotification(jobId: String, text: String, percent: Int, done: Boolean) {
        val job = queueStore.find(jobId) ?: return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId(job), notification(job, text, percent, done))
    }

    private fun notification(job: DownloadJob, text: String, percent: Int, done: Boolean): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this,
            job.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this,
            job.id.hashCode(),
            cancelIntent(this, job.id),
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
            builder.setOngoing(true).setProgress(100, percent.coerceIn(0, 100), false).addAction(0, getString(R.string.download_cancel), cancelIntent)
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

    private val DownloadJobState.isTerminal
        get() = this == DownloadJobState.COMPLETED ||
            this == DownloadJobState.CANCELLED ||
            this == DownloadJobState.FAILED

    companion object {
        private const val CHANNEL_ID = "atsu_player_downloads"
        private const val ACTION_START = "msr.atsulab.app.player.DOWNLOAD_START"
        private const val ACTION_CANCEL = "msr.atsulab.app.player.DOWNLOAD_CANCEL"
        private const val EXTRA_JOB_ID = "job_id"

        fun start(context: Context, queueStore: DownloadQueueStore, requests: List<DownloadRequest>): String {
            val job = queueStore.start(requests)
            val intent = Intent(context, PlayerDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_JOB_ID, job.id)
            }
            ContextCompat.startForegroundService(context, intent)
            return job.id
        }

        fun cancelIntent(context: Context, jobId: String): Intent {
            return Intent(context, PlayerDownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_JOB_ID, jobId)
            }
        }
    }
}
