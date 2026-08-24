package msr.atsulab.app.player.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import msr.atsulab.app.R

internal class PlayerControllerSkeleton(
    private val context: Context,
    val orientation: PlayerShellOrientation,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onBackClicked()
        fun onRetryClicked()
    }

    lateinit var titleView: TextView
        private set

    lateinit var statusView: TextView
        private set

    lateinit var retryButton: MaterialButton
        private set

    val topBar: LinearLayout = createTopBar()
    val statusControls: LinearLayout = createStatusControls()

    fun setStatus(message: String, showRetry: Boolean) {
        statusView.text = message
        retryButton.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun createTopBar(): LinearLayout {
        val density = context.resources.displayMetrics.density
        val isLandscape = orientation == PlayerShellOrientation.LANDSCAPE

        return LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            background = createTopGradient()
            setPadding(
                dp(if (isLandscape) 24 else 30, density),
                dp(if (isLandscape) 22 else 42, density),
                dp(if (isLandscape) 24 else 33, density),
                0
            )

            val backButton = ImageView(context).apply {
                setImageResource(R.drawable.ic_custom_close)
                setOnClickListener { callbacks.onBackClicked() }
            }
            addView(
                backButton,
                LinearLayout.LayoutParams(
                    dp(PlayerShellMetrics.BACK_BUTTON_SIZE_DP, density),
                    dp(PlayerShellMetrics.BACK_BUTTON_SIZE_DP, density)
                )
            )

            titleView = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = if (isLandscape) {
                    PlayerShellMetrics.LANDSCAPE_TITLE_TEXT_SIZE_SP.toFloat()
                } else {
                    PlayerShellMetrics.PORTRAIT_TITLE_TEXT_SIZE_SP.toFloat()
                }
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 1
            }
            val titleParams = LinearLayout.LayoutParams(0, dp(44, density), 1f)
            titleParams.setMargins(dp(18, density), 0, dp(8, density), 0)
            addView(titleView, titleParams)
        }
    }

    private fun createStatusControls(): LinearLayout {
        val density = context.resources.displayMetrics.density

        statusView = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(16, density), dp(12, density), dp(16, density), dp(12, density))
        }

        retryButton = MaterialButton(context).apply {
            setText(R.string.retry)
            visibility = View.GONE
            setOnClickListener { callbacks.onRetryClicked() }
        }

        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PlayerShellMetrics.SURFACE_COLOR)
            setPadding(dp(16, density), dp(12, density), dp(16, density), dp(12, density))
            addView(statusView)
            addView(retryButton)
        }
    }

    private fun createTopGradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xCC000000.toInt(), 0x00000000)
        )
    }

    private fun dp(value: Int, density: Float): Int {
        return (value * density).toInt()
    }
}
