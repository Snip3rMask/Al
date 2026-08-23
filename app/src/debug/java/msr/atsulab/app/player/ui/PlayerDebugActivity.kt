package msr.atsulab.app.player.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.engine.PlaybackEngine
import msr.atsulab.app.player.engine.PlaybackEngineListener
import msr.atsulab.app.player.engine.PlaybackReadyState
import msr.atsulab.app.player.engine.PlaybackState
import org.koin.core.context.GlobalContext

class PlayerDebugActivity : AppCompatActivity() {

    private var engine: PlaybackEngine? = null
    private var rootView: FrameLayout? = null
    private var statusView: TextView? = null
    private var startButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        PlayerDebugCrashRecorder.install(this)
        PlayerDebugCrashRecorder.breadcrumb(this, "activity_on_create")
        super.onCreate(savedInstanceState)

        PlayerDebugCrashRecorder.readReport(this)?.let { report ->
            PlayerDebugCrashRecorder.breadcrumb(this, "saved_report_found")
            showReport(report, showRetry = true)
            return
        }

        val statusTextView = TextView(this).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            text = buildString {
                appendLine(getString(msr.atsulab.app.R.string.player_debug_ready))
                appendLine()
                appendLine("LAST TRACE")
                append(PlayerDebugCrashRecorder.readTrace(this@PlayerDebugActivity).orEmpty())
                appendLine()
                appendLine("LOG PATHS")
                appendLine(PlayerDebugCrashRecorder.storagePaths(this@PlayerDebugActivity).joinToString("\n"))
            }
        }
        statusView = statusTextView

        val playButton = Button(this).apply {
            text = getString(msr.atsulab.app.R.string.player_debug_start)
            setOnClickListener { startStagedPlayback() }
        }
        startButton = playButton

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                ScrollView(this@PlayerDebugActivity).apply { addView(statusTextView) },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                playButton,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ).apply { setMargins(0, 0, 0, 80) }
            )
        }
        rootView = root
        setContentView(root)
        PlayerDebugCrashRecorder.breadcrumb(this, "activity_ui_ready")
    }

    private fun startStagedPlayback() {
        try {
            PlayerDebugCrashRecorder.breadcrumb(this, "start_tapped")
            startButton?.visibility = android.view.View.GONE

            val surfaceView = SurfaceView(this)
            PlayerDebugCrashRecorder.breadcrumb(this, "surface_created")
            rootView?.addView(
                surfaceView,
                0,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            PlayerDebugCrashRecorder.breadcrumb(this, "resolving_engine")
            val playbackEngine = GlobalContext.get().get<PlaybackEngine>()
            engine = playbackEngine
            PlayerDebugCrashRecorder.breadcrumb(this, "engine_resolved")

            playbackEngine.listener = object : PlaybackEngineListener {
                override fun onStateChanged(state: PlaybackState) {
                    PlayerDebugCrashRecorder.breadcrumb(
                        this@PlayerDebugActivity,
                        "state_${state.readyState.name.lowercase()}_playing_${state.isPlaying}"
                    )
                    when {
                        state.isPlaying -> statusView?.text = ""
                        state.readyState == PlaybackReadyState.BUFFERING ->
                            statusView?.text = getString(msr.atsulab.app.R.string.player_debug_buffering)

                        state.readyState == PlaybackReadyState.ENDED ->
                            statusView?.text = getString(msr.atsulab.app.R.string.player_debug_ended)
                    }
                }

                override fun onError(error: msr.atsulab.app.player.engine.PlaybackError) {
                    PlayerDebugCrashRecorder.breadcrumb(
                        this@PlayerDebugActivity,
                        "playback_error_${error.type.name}"
                    )
                    statusView?.text = getString(
                        msr.atsulab.app.R.string.player_debug_error_format,
                        error.type.name,
                        error.message
                    ) + (error.cause?.let { "\n\n${it.stackTraceToString()}" } ?: "")
                }
            }

            PlayerDebugCrashRecorder.breadcrumb(this, "attaching_surface")
            playbackEngine.setSurfaceView(surfaceView)
            PlayerDebugCrashRecorder.breadcrumb(this, "calling_prepare")
            playbackEngine.prepare(DEBUG_HLS_SOURCE)
            PlayerDebugCrashRecorder.breadcrumb(this, "prepare_returned")
        } catch (throwable: Throwable) {
            PlayerDebugCrashRecorder.breadcrumb(this, "caught_java_crash")
            PlayerDebugCrashRecorder.persist(this, Thread.currentThread(), throwable)
            showCrashReport(throwable)
        }
    }

    override fun onResume() {
        super.onResume()
        PlayerDebugCrashRecorder.breadcrumb(this, "activity_resume")
        runCatching { engine?.onForeground() }
    }

    override fun onStop() {
        super.onStop()
        PlayerDebugCrashRecorder.breadcrumb(this, "activity_stop")
        runCatching { engine?.onBackground() }
    }

    override fun onDestroy() {
        PlayerDebugCrashRecorder.breadcrumb(this, "activity_destroy")
        runCatching { engine?.release() }
        super.onDestroy()
    }

    private fun showCrashReport(throwable: Throwable) {
        releaseEngine()
        showReport(PlayerDebugCrashRecorder.readReport(this).orEmpty(), showRetry = true)
    }

    private fun showReport(report: String, showRetry: Boolean) {
        val reportView = TextView(this).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
            setTextColor(Color.WHITE)
            textSize = 12f
            text = buildString {
                appendLine(report.ifBlank { getString(msr.atsulab.app.R.string.player_debug_report_missing) })
                appendLine()
                appendLine("TRACE")
                append(PlayerDebugCrashRecorder.readTrace(this@PlayerDebugActivity).orEmpty())
                appendLine()
                appendLine("PATHS")
                appendLine(PlayerDebugCrashRecorder.storagePaths(this@PlayerDebugActivity).joinToString("\n"))
            }
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(Button(this@PlayerDebugActivity).apply {
                text = getString(msr.atsulab.app.R.string.player_debug_copy_log)
                setOnClickListener { copyReport(report) }
            })
            addView(Button(this@PlayerDebugActivity).apply {
                text = getString(msr.atsulab.app.R.string.player_debug_share_log)
                setOnClickListener { shareReport(report) }
            })
            if (showRetry) {
                addView(Button(this@PlayerDebugActivity).apply {
                    text = getString(msr.atsulab.app.R.string.player_debug_retry)
                    setOnClickListener {
                        PlayerDebugCrashRecorder.breadcrumb(this@PlayerDebugActivity, "retry_clear_started")
                        PlayerDebugCrashRecorder.clear(this@PlayerDebugActivity)
                        PlayerDebugCrashRecorder.breadcrumb(this@PlayerDebugActivity, "retry_clear_finished")
                        recreate()
                    }
                })
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                ScrollView(this@PlayerDebugActivity).apply { addView(reportView) },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
            addView(controls)
        }
        setContentView(content)
    }

    private fun copyReport(report: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AtsuLab HLS Crash", report))
        Toast.makeText(this, msr.atsulab.app.R.string.player_debug_log_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareReport(report: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AtsuLab HLS Debug Crash")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(intent, getString(msr.atsulab.app.R.string.player_debug_share_log)))
    }

    private fun releaseEngine() {
        runCatching { engine?.release() }
        engine = null
    }

    private companion object {
        const val DEBUG_HLS_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val DEBUG_HLS_SOURCE = VideoSource(quality = "Debug HLS", url = DEBUG_HLS_URL)
    }
}
