package msr.atsulab.app.player.download

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import msr.atsulab.app.R
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.ui.base.BaseActivityViewModel
import org.koin.android.ext.android.inject

class DownloadsActivity : AppCompatActivity() {

    private val queueStore: DownloadQueueStore by inject()
    private val entryStore: DownloadEntryStore by inject()
    private val userRepository: UserRepository by inject()

    private lateinit var content: LinearLayout
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            render()
            if (queueStore.activeJobs().isNotEmpty()) {
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(BaseActivityViewModel(userRepository).getAppThemeResource())
        setContentView(R.layout.activity_downloads)
        content = findViewById(R.id.downloadsContent)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.downloadsToolbar).apply {
            setTitleTextColor(resolveThemeColor(R.attr.themeContentColor))
            setNavigationOnClickListener { finish() }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRunnable.run()
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun render() {
        content.removeAllViews()
        val jobs = queueStore.activeJobs()
        val entries = entryStore.all().filter { it.file?.exists() == true }

        if (jobs.isNotEmpty()) {
            addSectionTitle(getString(R.string.downloads_active))
            jobs.forEach { job -> content.addView(activeJobView(job)) }
        }

        if (entries.isNotEmpty()) {
            addSectionTitle(
                getString(R.string.downloads_completed),
                topMargin = if (jobs.isNotEmpty()) 20.dp else 0
            )
            entries.forEach { entry -> content.addView(completedDownloadView(entry)) }
        }

        if (jobs.isEmpty() && entries.isEmpty()) {
            content.addView(emptyView())
        }
    }

    private fun addSectionTitle(text: String, spacingTop: Int = 0) {
        val title = TextView(this).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
            setTextColor(resolveThemeColor(R.attr.themeContentTransparentColor))
        }
        content.addView(title, linearParams(width = FrameLayout.LayoutParams.MATCH_PARENT) {
            topMargin = spacingTop
            bottomMargin = 10.dp
        })
    }

    private fun activeJobView(job: DownloadJob): View {
        val request = job.currentRequest
        return card {
            addView(titleView(request?.displayName ?: getString(R.string.download_notification_title)))
            addView(detailView("${getString(job.state.labelResId)} • ${job.percent}%"))
            addView(ProgressBar(this@DownloadsActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = job.percent
                isIndeterminate = false
            }, linearParams(FrameLayout.LayoutParams.MATCH_PARENT, 6.dp) { topMargin = 8.dp })
            addView(actionButton(getString(R.string.download_cancel)) {
                startService(PlayerDownloadService.cancelIntent(this@DownloadsActivity, job.id))
            }, linearParams(FrameLayout.LayoutParams.MATCH_PARENT, 38.dp) { topMargin = 10.dp })
        }
    }

    private fun completedDownloadView(entry: CompletedDownload): View {
        return card {
            setOnClickListener { openDownload(entry) }
            addView(titleView(entry.displayTitle))
            addView(detailView(entry.detail))
            val actions = LinearLayout(this@DownloadsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            actions.addView(actionButton(getString(R.string.download_open)) { openDownload(entry) })
            actions.addView(actionButton(getString(R.string.delete), destructive = true) {
                confirmDelete(entry)
            }, linearParams(FrameLayout.LayoutParams.WRAP_CONTENT, 38.dp) { leftMargin = 10.dp })
            addView(actions, linearParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT) { topMargin = 8.dp })
        }
    }

    private fun emptyView(): View {
        val emptyText = TextView(this).apply {
            text = getString(R.string.downloads_empty)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 48.dp, 0, 48.dp)
            setTextColor(resolveThemeColor(R.attr.themeContentTransparentColor))
        }
        return emptyText
    }

    private fun card(block: LinearLayout.() -> Unit): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp, 14.dp, 14.dp, 14.dp)
            background = GradientDrawable().apply {
                setColor(resolveThemeColor(R.attr.themeCardColor))
                cornerRadius = 12.dp.toFloat()
            }
            block()
        }
        return LinearLayout(this).also { wrapper ->
            wrapper.addView(card, linearParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT) { bottomMargin = 10.dp })
        }
    }

    private fun titleView(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resolveThemeColor(R.attr.themeContentColor))
        }
    }

    private fun detailView(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(resolveThemeColor(R.attr.themeContentTransparentColor))
            setPadding(0, 2.dp, 0, 0)
        }
    }

    private fun actionButton(
        text: String,
        destructive: Boolean = false,
        onClicked: () -> Unit
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(resolveThemeColor(if (destructive) R.attr.themeNegativeColor else R.attr.themeContentColor))
            background = GradientDrawable().apply {
                setColor(resolveThemeColor(R.attr.themeCardColor))
                cornerRadius = 19.dp.toFloat()
                setStroke(1.dp, resolveThemeColor(R.attr.themeDividerColor))
            }
            setOnClickListener { onClicked() }
        }
    }

    private fun openDownload(entry: CompletedDownload) {
        val file = entry.file
        if (file?.exists() != true) {
            entryStore.remove(entry.key)
            render()
            Toast.makeText(this, R.string.downloads_missing_file, Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.player.capture",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.downloads_no_player, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(entry: CompletedDownload) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_download_title)
            .setMessage(getString(R.string.delete_download_message, entry.displayTitle))
            .setPositiveButton(R.string.delete) { _, _ ->
                entry.file?.delete()
                entryStore.remove(entry.key)
                render()
            }
            .setNegativeButton(R.string.download_cancel, null)
            .show()
    }

    private fun resolveThemeColor(attributeId: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attributeId, value, true)
        return if (value.resourceId != 0) getColor(value.resourceId) else value.data
    }

    private val Int.dp: Int
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), resources.displayMetrics).toInt()

    private fun linearParams(
        width: Int,
        height: Int = FrameLayout.LayoutParams.WRAP_CONTENT,
        configureMargins: LinearLayout.LayoutParams.() -> Unit = {}
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(width, height).apply(configureMargins)
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 1_000L
    }
}

private val DownloadJobState.labelResId: Int
    get() = when (this) {
        DownloadJobState.QUEUED -> R.string.download_queued
        DownloadJobState.RUNNING, DownloadJobState.CANCELLING -> R.string.download_running
        DownloadJobState.COMPLETED -> R.string.download_completed
        DownloadJobState.CANCELLED -> R.string.download_cancelled
        DownloadJobState.FAILED -> R.string.download_failed
    }

private val CompletedDownload.displayTitle: String
    get() = "$title — Episode $episodeId"

private val CompletedDownload.detail: String
    get() {
        val megabytes = sizeBytes / (1024L * 1024L)
        val sizeLabel = if (megabytes > 0) "${megabytes}MB" else "1MB"
        return "$quality • $sizeLabel"
    }
