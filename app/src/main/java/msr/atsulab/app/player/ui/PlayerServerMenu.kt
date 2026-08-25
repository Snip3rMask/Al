package msr.atsulab.app.player.ui

import android.app.Activity
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import msr.atsulab.app.player.domain.model.VideoSource

internal class PlayerServerMenu(
    private val activity: Activity,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun videoSources(): List<VideoSource>
        fun selectedSourceIndex(): Int
        fun showDub(): Boolean
        fun onLanguageModeSelected(showDub: Boolean)
        fun onServerSelected(sourceIndex: Int)
        fun isMoreServersLoading(): Boolean
        fun hasAllServersFailed(): Boolean
        fun onRetryServersClicked()
        fun onServerMenuDismissed()
    }

    private var popup: PopupWindow? = null

    fun show(showLanguageTabs: Boolean) {
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
                text = if (showLanguageTabs) "Audio" else "Select Server"
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

        val showDub = callbacks.showDub()
        if (showLanguageTabs) {
            panel.addView(actionRow("SUB", isSelected = !showDub, density) {
                callbacks.onLanguageModeSelected(false)
                dismiss()
            }, linearRowParams(density))
            panel.addView(actionRow("DUB", isSelected = showDub, density) {
                callbacks.onLanguageModeSelected(true)
                dismiss()
            }, linearRowParams(density))
        }

        panel.addView(
            TextView(activity).apply {
                text = "Servers"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(18, density), 0, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52, density)
            )
        )

        val sources = callbacks.videoSources()
        PlayerServerMenuModel.options(
            sources = sources,
            selectedIndex = callbacks.selectedSourceIndex(),
            showDub = showDub
        ).forEach { option ->
            panel.addView(serverRow(option, density) {
                if (option.sourceIndex in sources.indices) {
                    callbacks.onServerSelected(option.sourceIndex)
                    dismiss()
                }
            }, linearRowParams(density))
        }

        if (callbacks.isMoreServersLoading()) {
            val loadingText = TextView(activity).apply {
                text = "Loading more servers..."
                textSize = 12f
                setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16, density), dp(10, density), dp(16, density), dp(10, density))
                alpha = 0.45f
            }
            val animator = ValueAnimator.ofFloat(0.45f, 1f).apply {
                duration = 700
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { animation -> loadingText.alpha = animation.animatedValue as Float }
            }
            loadingText.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) = animator.start()
                override fun onViewDetachedFromWindow(view: View) = animator.cancel()
            })
            if (loadingText.isAttachedToWindow) animator.start()
            panel.addView(
                loadingText,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54, density))
            )
        }

        val hasOptions = PlayerServerMenuModel.options(
            sources = callbacks.videoSources(),
            selectedIndex = callbacks.selectedSourceIndex(),
            showDub = callbacks.showDub()
        ).isNotEmpty()
        if (!hasOptions) {
            panel.addView(
                TextView(activity).apply {
                    text = if (callbacks.hasAllServersFailed()) "No working sources" else "No servers available"
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                    gravity = Gravity.CENTER_VERTICAL
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54, density))
            )
        }
        if (callbacks.hasAllServersFailed()) {
            panel.addView(actionRow("Retry", isSelected = false, density) {
                callbacks.onRetryServersClicked()
                dismiss()
            }, linearRowParams(density))
        }

        scroll.addView(
            panel,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
        root.addView(
            scroll,
            FrameLayout.LayoutParams(
                dp(PlayerShellMetrics.SERVER_PANEL_WIDTH_DP, density),
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
                callbacks.onServerMenuDismissed()
            }
        }
        popup = shownPopup

        root.alpha = 0f
        scroll.translationX = dp(PlayerShellMetrics.SERVER_PANEL_WIDTH_DP, density).toFloat()
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

    private fun actionRow(label: String, isSelected: Boolean, density: Float, onClick: () -> Unit): TextView {
        return TextView(activity).apply {
            text = label
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (isSelected) Color.WHITE else PlayerShellMetrics.MENU_TEXT_COLOR)
            background = GradientDrawable().apply {
                setColor(if (isSelected) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_BORDER_COLOR)
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

    private fun serverRow(option: ServerOption, density: Float, onClick: () -> Unit): TextView =
        actionRow(option.label, option.isSelected, density, onClick)

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
