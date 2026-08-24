package msr.atsulab.app.player.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import msr.atsulab.app.R

internal data class PlayerShellViews(
    val root: FrameLayout,
    val playerView: PlayerView,
    val controller: PlayerControllerSkeleton,
    val loadingIndicator: ProgressBar
)

internal class PlayerShellLayoutBuilder(
    private val context: Context,
    private val callbacks: PlayerControllerSkeleton.Callbacks
) {
    fun build(title: String, episodeLabel: String): PlayerShellViews {
        val orientation = PlayerShellOrientation.fromConfiguration(context.resources.configuration)
        val controller = PlayerControllerSkeleton(context, orientation, callbacks)
        controller.titleView.text = title

        val playerView = PlayerView(context).apply {
            useController = false
            keepScreenOn = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        val loadingIndicator = ProgressBar(context).apply { visibility = View.GONE }
        val videoFrame = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        videoFrame.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        return if (orientation == PlayerShellOrientation.LANDSCAPE) {
            buildLandscape(videoFrame, playerView, controller, loadingIndicator, episodeLabel)
        } else {
            buildPortrait(videoFrame, playerView, controller, loadingIndicator, episodeLabel)
        }
    }

    private fun buildPortrait(
        videoFrame: FrameLayout,
        playerView: PlayerView,
        controller: PlayerControllerSkeleton,
        loadingIndicator: ProgressBar,
        episodeLabel: String
    ): PlayerShellViews {
        val density = context.resources.displayMetrics.density
        val videoHeight = PlayerShellMetrics.portraitVideoHeightPixels(
            context.resources.displayMetrics.heightPixels
        )

        val topHeight = dp(
            if (orientation == PlayerShellOrientation.LANDSCAPE) {
                PlayerShellMetrics.LANDSCAPE_TOP_HEIGHT_DP
            } else {
                PlayerShellMetrics.PORTRAIT_TOP_HEIGHT_DP
            },
            density
        )
        videoFrame.addView(
            controller.topBar,
            FrameLayout.LayoutParams(MATCH_PARENT, topHeight, Gravity.TOP)
        )
        videoFrame.addView(
            loadingIndicator,
            FrameLayout.LayoutParams(dp(PlayerShellMetrics.LOADING_INDICATOR_SIZE_DP, density), dp(PlayerShellMetrics.LOADING_INDICATOR_SIZE_DP, density), Gravity.CENTER)
        )

        val watchingView = TextView(context).apply {
            text = episodeLabel
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(PlayerShellMetrics.SURFACE_COLOR)
        }

        val lowerScroll = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
        val lowerPanel = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        lowerPanel.setPadding(dp(37, density), dp(28, density), dp(37, density), dp(28, density))
        lowerPanel.addView(controller.statusControls)
        lowerScroll.addView(lowerPanel)

        val page = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        page.addView(
            videoFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, videoHeight)
        )
        page.addView(watchingView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(PlayerShellMetrics.WATCHING_ROW_HEIGHT_DP, density)))
        page.addView(lowerScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val root = FrameLayout(context).apply { setBackgroundColor(PlayerShellMetrics.PRIMARY_DARK_COLOR) }
        root.addView(page, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return PlayerShellViews(root, playerView, controller, loadingIndicator)
    }

    private fun buildLandscape(
        videoFrame: FrameLayout,
        playerView: PlayerView,
        controller: PlayerControllerSkeleton,
        loadingIndicator: ProgressBar,
        episodeLabel: String
    ): PlayerShellViews {
        val density = context.resources.displayMetrics.density
        videoFrame.addView(controller.topBar, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        videoFrame.addView(
            loadingIndicator,
            FrameLayout.LayoutParams(dp(PlayerShellMetrics.LOADING_INDICATOR_SIZE_DP, density), dp(PlayerShellMetrics.LOADING_INDICATOR_SIZE_DP, density), Gravity.CENTER)
        )

        controller.statusControls.setBackgroundColor(PlayerShellMetrics.SURFACE_COLOR)
        videoFrame.addView(
            controller.statusControls,
            FrameLayout.LayoutParams(MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        )

        val root = FrameLayout(context).apply { setBackgroundColor(PlayerShellMetrics.PRIMARY_DARK_COLOR) }
        root.addView(videoFrame, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return PlayerShellViews(root, playerView, controller, loadingIndicator)
    }

    private companion object {
        const val MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT
    }
}
