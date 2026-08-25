package msr.atsulab.app.player.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
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
    val videoFrame: FrameLayout,
    val playerView: PlayerView,
    val brightnessScrimView: View,
    val controller: PlayerControllerSkeleton,
    val loadingIndicator: ProgressBar,
    val gestureHudView: PlayerGestureHudView,
    val skipButton: TextView,
    val captureButton: ImageView?,
    val watchingView: TextView?,
    val portraitContent: PlayerPortraitContent?
)

internal interface PlayerShellCallbacks : PlayerControllerSkeleton.Callbacks, PlayerPortraitContent.Callbacks

internal class PlayerShellLayoutBuilder(
    private val context: Context,
    private val callbacks: PlayerShellCallbacks
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

        val brightnessScrimView = View(context).apply {
            setBackgroundColor(Color.BLACK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        val loadingIndicator = ProgressBar(context).apply { visibility = View.GONE }
        val gestureHudView = PlayerGestureHudView(context).apply { visibility = View.GONE }
        val density = context.resources.displayMetrics.density
        val skipButton = TextView(context).apply {
            background = GradientDrawable().apply {
                setColor(PlayerShellMetrics.ACCENT_COLOR)
                cornerRadius =
                    dp(PlayerShellMetrics.SKIP_BUTTON_HEIGHT_DP, density) / 2f
            }
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(
                dp(PlayerShellMetrics.SKIP_BUTTON_HORIZONTAL_PADDING_DP, density),
                0,
                dp(PlayerShellMetrics.SKIP_BUTTON_HORIZONTAL_PADDING_DP, density),
                0
            )
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val videoFrame = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
        }
        videoFrame.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        videoFrame.addView(
            brightnessScrimView,
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )

        return if (orientation == PlayerShellOrientation.LANDSCAPE) {
            buildLandscape(
                videoFrame,
                playerView,
                brightnessScrimView,
                controller,
                loadingIndicator,
                gestureHudView,
                skipButton,
                episodeLabel
            )
        } else {
            buildPortrait(
                videoFrame,
                playerView,
                brightnessScrimView,
                controller,
                loadingIndicator,
                gestureHudView,
                skipButton,
                episodeLabel
            )
        }
    }

    private fun buildPortrait(
        videoFrame: FrameLayout,
        playerView: PlayerView,
        brightnessScrimView: View,
        controller: PlayerControllerSkeleton,
        loadingIndicator: ProgressBar,
        gestureHudView: PlayerGestureHudView,
        skipButton: TextView,
        episodeLabel: String
    ): PlayerShellViews {
        val density = context.resources.displayMetrics.density
        val videoHeight = PlayerShellMetrics.portraitVideoHeightPixels(
            context.resources.displayMetrics.heightPixels
        )

        val topHeight = dp(
            if (controller.orientation == PlayerShellOrientation.LANDSCAPE) {
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
        videoFrame.addView(
            controller.transportControls,
            FrameLayout.LayoutParams(
                MATCH_PARENT,
                dp(PlayerShellMetrics.BOTTOM_CONTROLS_HEIGHT_DP, density),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        )
        videoFrame.addView(
            controller.unlockButton,
            FrameLayout.LayoutParams(
                dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                Gravity.TOP or Gravity.START
            ).apply {
                setMargins(
                    dp(PlayerShellMetrics.UNLOCK_BUTTON_LEFT_MARGIN_DP, density),
                    dp(PlayerShellMetrics.UNLOCK_BUTTON_PORTRAIT_TOP_MARGIN_DP, density),
                    0,
                    0
                )
            }
        )

        videoFrame.addView(
            gestureHudView,
            FrameLayout.LayoutParams(
                dp(PlayerShellMetrics.GESTURE_HUD_WIDTH_DP, density),
                dp(PlayerShellMetrics.GESTURE_HUD_HEIGHT_DP, density),
                Gravity.START or Gravity.CENTER_VERTICAL
            ).apply {
                setMargins(dp(PlayerShellMetrics.GESTURE_HUD_SIDE_MARGIN_DP, density), 0, 0, 0)
            }
        )

        videoFrame.addView(
            skipButton,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(PlayerShellMetrics.SKIP_BUTTON_HEIGHT_DP, density),
                Gravity.BOTTOM or Gravity.END
            ).apply {
                setMargins(
                    0,
                    0,
                    dp(PlayerShellMetrics.SKIP_BUTTON_END_MARGIN_DP, density),
                    dp(PlayerShellMetrics.SKIP_BUTTON_BOTTOM_MARGIN_DP, density)
                )
            }
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
        val portraitContent = if (controller.orientation == PlayerShellOrientation.PORTRAIT) {
            PlayerPortraitContent(context, callbacks)
        } else {
            null
        }
        portraitContent?.let(lowerPanel::addView)
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
        return PlayerShellViews(
            root,
            videoFrame,
            playerView,
            brightnessScrimView,
            controller,
            loadingIndicator,
            gestureHudView,
            skipButton,
            null,
            watchingView,
            portraitContent
        )
    }

    private fun buildLandscape(
        videoFrame: FrameLayout,
        playerView: PlayerView,
        brightnessScrimView: View,
        controller: PlayerControllerSkeleton,
        loadingIndicator: ProgressBar,
        gestureHudView: PlayerGestureHudView,
        skipButton: TextView,
        episodeLabel: String
    ): PlayerShellViews {
        val density = context.resources.displayMetrics.density
        videoFrame.addView(
            controller.topBar,
            FrameLayout.LayoutParams(
                MATCH_PARENT,
                dp(PlayerShellMetrics.LANDSCAPE_TOP_HEIGHT_DP, density),
                Gravity.TOP
            )
        )
        videoFrame.addView(
            loadingIndicator,
            FrameLayout.LayoutParams(dp(PlayerShellMetrics.LOADING_INDICATOR_SIZE_DP, density), dp(PlayerShellMetrics.LOADING_INDICATOR_SIZE_DP, density), Gravity.CENTER)
        )

        controller.statusControls.setBackgroundColor(PlayerShellMetrics.SURFACE_COLOR)
        videoFrame.addView(
            controller.statusControls,
            FrameLayout.LayoutParams(MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        videoFrame.addView(
            controller.transportControls,
            FrameLayout.LayoutParams(
                MATCH_PARENT,
                dp(PlayerShellMetrics.BOTTOM_CONTROLS_HEIGHT_DP, density),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        )
        videoFrame.addView(
            controller.unlockButton,
            FrameLayout.LayoutParams(
                dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                Gravity.TOP or Gravity.START
            ).apply {
                setMargins(
                    dp(PlayerShellMetrics.UNLOCK_BUTTON_LEFT_MARGIN_DP, density),
                    dp(PlayerShellMetrics.UNLOCK_BUTTON_LANDSCAPE_TOP_MARGIN_DP, density),
                    0,
                    0
                )
            }
        )

        videoFrame.addView(
            gestureHudView,
            FrameLayout.LayoutParams(
                dp(PlayerShellMetrics.GESTURE_HUD_WIDTH_DP, density),
                dp(PlayerShellMetrics.GESTURE_HUD_HEIGHT_DP, density),
                Gravity.END or Gravity.CENTER_VERTICAL
            ).apply {
                setMargins(0, 0, dp(PlayerShellMetrics.GESTURE_HUD_SIDE_MARGIN_DP, density), 0)
            }
        )

        videoFrame.addView(
            skipButton,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(PlayerShellMetrics.SKIP_BUTTON_HEIGHT_DP, density),
                Gravity.BOTTOM or Gravity.END
            ).apply {
                setMargins(
                    0,
                    0,
                    dp(PlayerShellMetrics.SKIP_BUTTON_END_MARGIN_DP, density),
                    dp(PlayerShellMetrics.SKIP_BUTTON_BOTTOM_MARGIN_DP, density)
                )
            }
        )

        val captureButton = ImageView(context).apply {
            setImageResource(R.drawable.ic_player_capture_modern)
            contentDescription = context.getString(R.string.player_capture_frame)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundResource(R.drawable.ripple_circle)
            setPadding(dp(8, density), dp(8, density), dp(8, density), dp(8, density))
        }
        videoFrame.addView(
            captureButton,
            FrameLayout.LayoutParams(
                dp(42, density),
                dp(42, density),
                Gravity.TOP or Gravity.END
            ).apply {
                setMargins(0, dp(86, density), dp(24, density), 0)
            }
        )
        controller.setCaptureButton(captureButton)

        val root = FrameLayout(context).apply { setBackgroundColor(PlayerShellMetrics.PRIMARY_DARK_COLOR) }
        root.addView(videoFrame, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return PlayerShellViews(
            root,
            videoFrame,
            playerView,
            brightnessScrimView,
            controller,
            loadingIndicator,
            gestureHudView,
            skipButton,
            captureButton,
            null,
            null
        )
    }

    private fun dp(value: Int, density: Float): Int {
        return (value * density).toInt()
    }

    private companion object {
        const val MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT
    }
}
