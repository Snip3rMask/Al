package msr.atsulab.app.player.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
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
    private var statusView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surfaceView = SurfaceView(this)
        val statusTextView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
            gravity = Gravity.CENTER
            text = getString(msr.atsulab.app.R.string.player_debug_loading)
        }
        statusView = statusTextView

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                statusTextView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        setContentView(root)

        try {
            startPlayback(surfaceView)
        } catch (throwable: Throwable) {
            showCrashReport(throwable)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { engine?.onBackground() }
    }

    override fun onResume() {
        super.onResume()
        runCatching { engine?.onForeground() }
    }

    override fun onDestroy() {
        runCatching { engine?.release() }
        super.onDestroy()
    }

    private fun startPlayback(surfaceView: SurfaceView) {
        val playbackEngine = GlobalContext.get().get<PlaybackEngine>()
        engine = playbackEngine

        playbackEngine.listener = object : PlaybackEngineListener {
            override fun onStateChanged(state: PlaybackState) {
                when {
                    state.isPlaying -> statusView?.text = ""
                    state.readyState == PlaybackReadyState.BUFFERING ->
                        statusView?.text = getString(msr.atsulab.app.R.string.player_debug_buffering)

                    state.readyState == PlaybackReadyState.ENDED ->
                        statusView?.text = getString(msr.atsulab.app.R.string.player_debug_ended)
                }
            }

            override fun onError(error: msr.atsulab.app.player.engine.PlaybackError) {
                val details = getString(
                    msr.atsulab.app.R.string.player_debug_error_format,
                    error.type.name,
                    error.message
                )
                statusView?.text = listOf(details, error.cause?.let(::stackTrace)).filterNotNull().joinToString("\n\n")
            }
        }

        playbackEngine.setSurfaceView(surfaceView)
        playbackEngine.prepare(DEBUG_HLS_SOURCE)
    }

    private fun showCrashReport(throwable: Throwable) {
        engine?.release()
        engine = null

        val report = buildString {
            appendLine("AtsuLab HLS Debug Crash")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Package: $packageName")
            appendLine()
            append(stackTrace(throwable))
        }

        val reportView = TextView(this).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
            setTextColor(Color.WHITE)
            textSize = 12f
            text = report
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
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                ScrollView(this@PlayerDebugActivity).apply {
                    addView(reportView)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
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

    private fun stackTrace(throwable: Throwable): String {
        return java.io.StringWriter().also { writer ->
            throwable.printStackTrace(java.io.PrintWriter(writer))
        }.toString()
    }

    private companion object {
        const val DEBUG_HLS_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val DEBUG_HLS_SOURCE = VideoSource(quality = "Debug HLS", url = DEBUG_HLS_URL)
    }
}
