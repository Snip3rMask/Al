package msr.atsulab.app.player.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import msr.atsulab.app.player.domain.PlaybackSpeedOptions

internal class PlayerSpeedMenu(
    private val activity: Activity,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun currentSpeed(): Float
        fun onSpeedSelected(speed: Float)
        fun onSpeedMenuDismissed()
    }

    private var popup: PopupWindow? = null

    val isShowing: Boolean
        get() = popup?.isShowing == true

    fun show() {
        dismiss()
        val density = activity.resources.displayMetrics.density
        val list = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(PlayerShellMetrics.SPEED_MENU_PADDING_DP, density),
                dp(PlayerShellMetrics.SPEED_MENU_PADDING_DP, density),
                dp(PlayerShellMetrics.SPEED_MENU_PADDING_DP, density),
                dp(PlayerShellMetrics.SPEED_MENU_PADDING_DP, density)
            )
            background = GradientDrawable().apply {
                setColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
                cornerRadius = dp(12, density).toFloat()
                setStroke(dp(1, density), PlayerShellMetrics.MENU_BORDER_COLOR)
            }
        }
        val currentSpeed = callbacks.currentSpeed()

        PlaybackSpeedOptions.VALUES.forEach { speed ->
            val isSelected = PlaybackSpeedOptions.isSelected(currentSpeed, speed)
            val row = TextView(activity).apply {
                text = PlaybackSpeedOptions.label(speed)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(
                    if (isSelected) Color.WHITE else PlayerShellMetrics.MENU_TEXT_COLOR
                )
                background = GradientDrawable().apply {
                    setColor(if (isSelected) PlayerShellMetrics.ACCENT_COLOR else Color.TRANSPARENT)
                    cornerRadius = dp(9, density).toFloat()
                }
                setPadding(dp(18, density), 0, dp(14, density), 0)
                isFocusable = true
                isFocusableInTouchMode = false
                setOnFocusChangeListener { view, hasFocus ->
                    view.foreground = if (hasFocus) focusOutline(density) else null
                }
                setOnClickListener {
                    callbacks.onSpeedSelected(speed)
                    dismiss()
                }
            }
            val rowParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(PlayerShellMetrics.SPEED_MENU_ROW_HEIGHT_DP, density)
            )
            rowParams.setMargins(
                0,
                dp(PlayerShellMetrics.SPEED_MENU_ROW_MARGIN_DP, density),
                0,
                dp(PlayerShellMetrics.SPEED_MENU_ROW_MARGIN_DP, density)
            )
            list.addView(row, rowParams)
        }

        val shownPopup = PopupWindow(
            list,
            dp(PlayerShellMetrics.SPEED_MENU_WIDTH_DP, density),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            outsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnDismissListener {
                popup = null
                callbacks.onSpeedMenuDismissed()
            }
        }
        popup = shownPopup

        shownPopup.showAtLocation(
            activity.window.decorView,
            Gravity.RIGHT or Gravity.TOP,
            dp(PlayerShellMetrics.SPEED_MENU_HORIZONTAL_OFFSET_DP, density),
            dp(PlayerShellMetrics.SPEED_MENU_VERTICAL_OFFSET_DP, density)
        )

        list.translationX = dp(PlayerShellMetrics.SPEED_MENU_ANIMATION_OFFSET_DP, density).toFloat()
        list.alpha = 0f
        list.animate().translationX(0f).alpha(1f).setDuration(170).start()
    }

    fun dismiss(notifyCallbacks: Boolean = true) {
        val currentPopup = popup ?: return
        if (!notifyCallbacks) {
            currentPopup.setOnDismissListener(null)
        }
        if (currentPopup.isShowing) {
            currentPopup.dismiss()
        }
        popup = null
    }

    private fun focusOutline(density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(100, density).toFloat()
            setStroke(dp(2, density), Color.WHITE)
        }
    }

    private fun dp(value: Int, density: Float): Int {
        return (value * density).toInt()
    }
}
