package msr.atsulab.app.player.ui

import android.app.Activity
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
import android.widget.Switch
import android.widget.TextView
import msr.atsulab.app.R

internal class PlayerFrameCaptureSettingsMenu(
    private val activity: Activity,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun isEnabled(): Boolean
        fun isAlwaysVisible(): Boolean
        fun onEnabledChanged(enabled: Boolean)
        fun onAlwaysVisibleChanged(enabled: Boolean)
        fun onSettingsDismissed()
    }

    private var popup: PopupWindow? = null

    val isShowing: Boolean
        get() = popup?.isShowing == true

    fun show() {
        dismiss(notifyCallbacks = false)
        val density = activity.resources.displayMetrics.density
        val root = FrameLayout(activity).apply {
            setBackgroundColor(0x66000000)
            fitsSystemWindows = false
            clipToPadding = false
            setOnClickListener { dismiss() }
        }
        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            isFocusable = false
            clipToPadding = false
            setBackgroundColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
        }
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28, density), dp(34, density), dp(28, density), dp(20, density))
            setOnClickListener { }
        }

        panel.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.player_settings)
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58, density)
            )
        )
        panel.addView(
            toggleRow(
                activity.getString(R.string.player_capture_frame),
                callbacks.isEnabled(),
                density
            ) { enabled -> callbacks.onEnabledChanged(enabled) },
            rowParams(density)
        )
        panel.addView(
            toggleRow(
                activity.getString(R.string.player_capture_always_visible),
                callbacks.isAlwaysVisible(),
                density
            ) { enabled -> callbacks.onAlwaysVisibleChanged(enabled) },
            rowParams(density)
        )

        scroll.addView(
            panel,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
        root.addView(
            scroll,
            FrameLayout.LayoutParams(dp(330, density), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT)
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
                callbacks.onSettingsDismissed()
            }
        }
        popup = shownPopup
        root.alpha = 0f
        scroll.translationX = dp(330, density).toFloat()
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

    private fun toggleRow(
        title: String,
        checked: Boolean,
        density: Float,
        onChanged: (Boolean) -> Unit
    ): LinearLayout {
        val switch = Switch(activity).apply {
            isChecked = checked
            isFocusable = false
        }
        return LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(PlayerShellMetrics.SURFACE_COLOR)
                cornerRadius = dp(12, density).toFloat()
            }
            setPadding(dp(16, density), 0, dp(16, density), 0)
            addView(
                TextView(activity).apply {
                    text = title
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(switch)
            setOnClickListener { switch.isChecked = !switch.isChecked }
            switch.setOnCheckedChangeListener { _, isChecked -> onChanged(isChecked) }
        }
    }

    private fun rowParams(density: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(66, density)
        ).apply { setMargins(0, dp(8, density), 0, dp(8, density)) }
    }

    private fun dp(value: Int, density: Float): Int = (value * density).toInt()
}
