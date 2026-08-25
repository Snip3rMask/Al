package msr.atsulab.app.player.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import msr.atsulab.app.player.domain.model.PlaybackEpisode

internal class PlayerEpisodePanel(
    private val activity: Activity,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun episodes(): List<PlaybackEpisode>
        fun currentEpisode(): PlaybackEpisode?
        fun rangeStart(): Int
        fun onRangeSelected(rangeStart: Int)
        fun onEpisodeSelected(episode: PlaybackEpisode)
        fun onEpisodePanelDismissed()
    }

    private var popup: PopupWindow? = null
    private var rangePopup: PopupWindow? = null

    fun show() {
        dismiss(notifyCallbacks = false)
        val density = activity.resources.displayMetrics.density
        val panelWidth = minOf(
            dp(PlayerShellMetrics.EPISODE_PANEL_MAX_WIDTH_DP, density),
            maxOf(
                dp(PlayerShellMetrics.EPISODE_PANEL_MIN_WIDTH_DP, density),
                activity.resources.displayMetrics.widthPixels / 3
            )
        )
        val root = FrameLayout(activity).apply {
            setBackgroundColor(0x88000000.toInt())
            setOnClickListener { dismiss() }
        }
        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            isFocusable = false
            setBackgroundColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
            setOnClickListener { }
        }
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28, density), dp(26, density), dp(28, density), dp(26, density))
            setOnClickListener { }
        }
        scroll.addView(panel)

        val header = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(TextView(activity).apply {
            text = activity.getString(msr.atsulab.app.R.string.episodes)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, dp(54, density), 1f))
        header.addView(TextView(activity).apply {
            text = "${callbacks.episodes().size} ${activity.getString(msr.atsulab.app.R.string.episodes)}"
            textSize = 14f
            setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        })
        panel.addView(header)

        val rangeButton = TextView(activity).apply {
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16, density), dp(9, density), dp(16, density), dp(9, density))
            background = GradientDrawable().apply {
                setColor(PlayerShellMetrics.MENU_BORDER_COLOR)
                cornerRadius = dp(100, density).toFloat()
            }
            setOnClickListener { anchor -> showRangePopup(anchor) }
        }
        updateRangeButton(rangeButton)
        panel.addView(rangeButton, linearParams(density))

        val grid = GridLayout(activity).apply { columnCount = GRID_COLUMN_COUNT }
        renderGrid(grid, density, panelWidth)
        panel.addView(grid)

        root.addView(scroll, FrameLayout.LayoutParams(panelWidth, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT))
        scroll.translationX = panelWidth.toFloat()

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
                callbacks.onEpisodePanelDismissed()
            }
        }
        popup = shownPopup
        root.alpha = 0f
        shownPopup.showAtLocation(activity.window.decorView, Gravity.RIGHT, 0, 0)
        root.systemUiVisibility = activity.window.decorView.systemUiVisibility
        root.animate().alpha(1f).setDuration(120).start()
        scroll.animate().translationX(0f).setDuration(210).start()
    }

    fun refreshIfShowing() {
        if (popup?.isShowing == true) show()
    }

    fun dismiss(notifyCallbacks: Boolean = true) {
        dismissRangePopup()
        val currentPopup = popup ?: return
        if (!notifyCallbacks) currentPopup.setOnDismissListener(null)
        if (currentPopup.isShowing) currentPopup.dismiss()
        popup = null
    }

    private fun renderGrid(grid: GridLayout, density: Float, panelWidth: Int) {
        grid.removeAllViews()
        val horizontalPadding = dp(28, density) * 2
        val cellMargin = dp(5, density)
        val cellWidth = maxOf(
            dp(40, density),
            (panelWidth - horizontalPadding) / GRID_COLUMN_COUNT - cellMargin * 2
        )
        val options = PlayerEpisodePanelModel.gridOptions(
            episodes = callbacks.episodes(),
            currentEpisode = callbacks.currentEpisode(),
            selectedRangeStart = callbacks.rangeStart()
        )
        options.forEach { option ->
            val episodeView = TextView(activity).apply {
                text = option.number.toString()
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(if (option.isSelected) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_BORDER_COLOR)
                    cornerRadius = dp(8, density).toFloat()
                }
                setOnClickListener {
                    callbacks.onEpisodeSelected(option.episode)
                    dismiss()
                }
            }
            grid.addView(episodeView, GridLayout.LayoutParams().apply {
                width = cellWidth
                height = dp(66, density)
                setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
            })
        }
        if (options.isEmpty()) {
            grid.addView(TextView(activity).apply {
                text = activity.getString(msr.atsulab.app.R.string.player_no_episodes_in_range)
                textSize = 16f
                setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
            })
        }
    }

    private fun updateRangeButton(button: TextView) {
        button.text = PlayerEpisodePanelModel.controlLabel(
            PlayerEpisodePanelModel.ranges(callbacks.episodes(), callbacks.rangeStart())
        )
    }

    private fun showRangePopup(anchor: View) {
        dismissRangePopup()
        val density = activity.resources.displayMetrics.density
        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setBackgroundResource(android.R.color.transparent)
        }
        val list = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8, density), 0, dp(8, density))
        }
        val ranges = PlayerEpisodePanelModel.ranges(callbacks.episodes(), callbacks.rangeStart())
        ranges.forEach { range ->
            val row = LinearLayout(activity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18, density), dp(10, density), dp(14, density), dp(10, density))
                background = if (range.isSelected) GradientDrawable().apply {
                    setColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
                    cornerRadius = dp(10, density).toFloat()
                } else {
                    null
                }
                setOnClickListener {
                    callbacks.onRangeSelected(range.start)
                    dismissRangePopup()
                }
            }
            row.addView(TextView(activity).apply {
                text = "EPS ${range.start} - ${range.endInclusive}"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (range.isSelected) Color.WHITE else PlayerShellMetrics.MENU_TEXT_COLOR)
            }, LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(activity).apply {
                text = "${range.endInclusive - range.start + 1} eps"
                textSize = 11f
                setTextColor(if (range.isSelected) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_TEXT_COLOR)
                gravity = Gravity.CENTER_VERTICAL
            })
            list.addView(row, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(8, density), dp(4, density), dp(8, density), dp(4, density))
            })
        }
        scroll.addView(list)
        val maxHeight = minOf(dp(420, density), activity.resources.displayMetrics.heightPixels / 2)
        val shownPopup = PopupWindow(scroll, dp(280, density), minOf(maxHeight, ranges.size * dp(56, density) + dp(16, density)), true).apply {
            isOutsideTouchable = true
            elevation = dp(16, density).toFloat()
            setOnDismissListener { rangePopup = null }
        }
        rangePopup = shownPopup
        shownPopup.showAsDropDown(anchor, 0, dp(8, density))
    }

    private fun dismissRangePopup() {
        rangePopup?.dismiss()
        rangePopup = null
    }

    private fun linearParams(density: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16, density)
        }

    private fun dp(value: Int, density: Float): Int = (value * density).toInt()

    private companion object {
        const val GRID_COLUMN_COUNT = 4
    }
}
