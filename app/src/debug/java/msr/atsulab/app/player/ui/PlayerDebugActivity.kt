package msr.atsulab.app.player.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import msr.atsulab.app.player.domain.model.VideoSource
import msr.atsulab.app.player.engine.PlaybackEngine
import msr.atsulab.app.player.engine.PlaybackEngineListener
import msr.atsulab.app.player.engine.PlaybackState
import org.koin.android.ext.android.inject

class PlayerDebugActivity : AppCompatActivity() {

    private val engine: PlaybackEngine by inject()
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surfaceView = SurfaceView(this)
        statusView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
            gravity = Gravity.CENTER
            text = getString(msr.atsulab.app.R.string.player_debug_loading)
        }
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
                statusView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        setContentView(root)

        engine.listener = object : PlaybackEngineListener {
            override fun onStateChanged(state: PlaybackState) {
                when {
                    state.isPlaying -> statusView.text = ""
                    state.readyState == msr.atsulab.app.player.engine.PlaybackReadyState.BUFFERING ->
                        statusView.text = getString(msr.atsulab.app.R.string.player_debug_buffering)
                    state.readyState == msr.atsulab.app.player.engine.PlaybackReadyState.ENDED ->
                        statusView.text = getString(msr.atsulab.app.R.string.player_debug_ended)
                }
            }

            override fun onError(error: msr.atsulab.app.player.engine.PlaybackError) {
                statusView.text = getString(
                    msr.atsulab.app.R.string.player_debug_error_format,
                    error.type.name,
                    error.message
                )
            }
        }
        engine.setSurfaceView(surfaceView)
        engine.prepare(DEBUG_HLS_SOURCE)
    }

    override fun onStop() {
        super.onStop()
        engine.onBackground()
    }

    override fun onResume() {
        super.onResume()
        engine.onForeground()
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }

    private companion object {
        const val DEBUG_HLS_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        val DEBUG_HLS_SOURCE = VideoSource(quality = "Debug HLS", url = DEBUG_HLS_URL)
    }
}
