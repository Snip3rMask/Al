package msr.atsulab.app.player.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import msr.atsulab.app.player.domain.model.SubtitleTrack

internal class PlayerSubtitleMenu(
    private val activity: Activity,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun subtitleTracks(): List<SubtitleTrack>
        fun hasExternalSubtitle(): Boolean
        fun onSubtitleSelected(trackId: String?)
        fun onSubtitleMenuDismissed()
    }

    private var popup: PopupWindow? = null

    val isShowing: Boolean
        get() = popup?.isShowing == true

    fun show() {
        dismiss()
        val density = activity.resources.displayMetrics.density
        val root = FrameLayout(activity).apply {
            setBackgroundColor(0x66000000)
            fitsSystemWindows = false
            clipToPadding = false
            clipChildren = false
            setOnClickListener { dismiss() }
        }

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            isFocusable = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(PlayerShellMetrics.SUBTITLE_PANEL_BOTTOM_PADDING_DP, density))
            setBackgroundColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
        }

        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(PlayerShellMetrics.SUBTITLE_PANEL_HORIZONTAL_PADDING_DP, density),
                dp(PlayerShellMetrics.SUBTITLE_PANEL_TOP_PADDING_DP, density),
                dp(PlayerShellMetrics.SUBTITLE_PANEL_HORIZONTAL_PADDING_DP, density),
                dp(PlayerShellMetrics.SUBTITLE_PANEL_BOTTOM_PADDING_DP, density)
            )
            setOnClickListener { }
        }

        panel.addView(
            TextView(activity).apply {
                text = "Subtitles"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(PlayerShellMetrics.SUBTITLE_TITLE_HEIGHT_DP, density)
            )
        )

        PlayerSubtitleMenuModel.options(
            tracks = callbacks.subtitleTracks(),
            hasExternalSubtitle = callbacks.hasExternalSubtitle()
        ).forEach { option ->
            panel.addView(
                subtitleRow(option, density) {
                    callbacks.onSubtitleSelected(option.id)
                    dismiss()
                },
                linearRowParams(density)
            )
        }

        scroll.addView(
            panel,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
        root.addView(
            scroll,
            FrameLayout.LayoutParams(
                dp(PlayerShellMetrics.SUBTITLE_PANEL_WIDTH_DP, density),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.RIGHT
            )
        )

        val shownPopup = PopupWindow().apply {
            contentView = root
            width = FrameLayout.LayoutParams.MATCH_PARENT
            height = FrameLayout.LayoutParams.MATCH_PARENT
            isFocusable = true
            isOutsideTouchable = true
            isClippingEnabled = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            setOnDismissListener {
                popup = null
                callbacks.onSubtitleMenuDismissed()
            }
        }
        popup = shownPopup

        root.alpha = 0f
        scroll.translationX = dp(PlayerShellMetrics.SUBTITLE_PANEL_WIDTH_DP, density).toFloat()
        shownPopup.showAtLocation(activity.window.decorView, Gravity.RIGHT, 0, 0)
        root.systemUiVisibility = activity.window.decorView.systemUiVisibility
        root.animate().alpha(1f).setDuration(120).start()
        scroll.animate().translationX(0f).setDuration(210).start()
    }

    fun dismiss(notifyCallbacks: Boolean = true) {
        val currentPopup = popup ?: return
        if (!notifyCallbacks) currentPopup.setOnDismissListener(null)
        if (currentPopup.isShowing) currentPopup.dismiss()
        popup = null
    }

    private fun subtitleRow(option: SubtitleTrackOption, density: Float, onClick: () -> Unit): TextView {
        return TextView(activity).apply {
            text = option.label
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (option.isSelected) Color.WHITE else PlayerShellMetrics.MENU_TEXT_COLOR)
            background = GradientDrawable().apply {
                setColor(
                    if (option.isSelected) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_BORDER_COLOR
                )
                cornerRadius = dp(8, density).toFloat()
            }
            isFocusable = true
            isFocusableInTouchMode = false
            setOnFocusChangeListener { view, hasFocus ->
                view.foreground = if (hasFocus) focusOutline(density) else null
            }
            setOnClickListener { onClick() }
        }
    }

    private fun linearRowParams(density: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(PlayerShellMetrics.SUBTITLE_ROW_HEIGHT_DP, density)
        ).apply {
            setMargins(0, dp(PlayerShellMetrics.SUBTITLE_ROW_MARGIN_DP, density), 0, dp(PlayerShellMetrics.SUBTITLE_ROW_MARGIN_DP, density))
        }
    }

    private fun focusOutline(density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(100, density).toFloat()
            setStroke(dp(2, density), Color.WHITE)
        }
    }

    private fun dp(value: Int, density: Float): Int = (value * density).toInt()
}
