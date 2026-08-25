package msr.atsulab.app.player.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import msr.atsulab.app.player.domain.SubtitleStyleOptions
import msr.atsulab.app.player.domain.model.SubtitleStyle

internal class PlayerSubtitleStylePanel(
    private val activity: Activity,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun currentSubtitleStyle(): SubtitleStyle
        fun onSubtitleStyleChanged(style: SubtitleStyle)
        fun onCustomFontClicked()
        fun onClearCustomFontClicked()
        fun onSubtitleStyleDismissed()
    }

    private var popup: PopupWindow? = null

    val isShowing: Boolean
        get() = popup?.isShowing == true

    fun show() {
        dismiss(notifyCallbacks = false)
        var style = SubtitleStyleOptions.normalize(callbacks.currentSubtitleStyle())
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

        panel.addView(title("Subtitle style"), titleParams(density))
        panel.addView(sectionLabel("Text Size"), sectionMargin(density, 26))
        val sizeValue = TextView(activity).apply {
            text = "${SubtitleStyleOptions.fontSizeToPercent(style.fontSize)}%"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(PlayerShellMetrics.ACCENT_COLOR)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        panel.addView(
            sliderRow(
                valueView = sizeValue,
                initialValue = SubtitleStyleOptions.fontSizeToPercent(style.fontSize),
                minimum = SubtitleStyleOptions.MIN_FONT_SIZE_PERCENT,
                maximum = SubtitleStyleOptions.MAX_FONT_SIZE_PERCENT,
                suffix = "%",
                density = density,
                onChanged = { value ->
                    val percent = SubtitleStyleOptions.fontSizeToPercent(
                        SubtitleStyleOptions.percentToFontSize(value)
                    )
                    sizeValue.text = "$percent%"
                    style = SubtitleStyleOptions.normalize(
                        style.copy(fontSize = SubtitleStyleOptions.percentToFontSize(value))
                    )
                    callbacks.onSubtitleStyleChanged(style)
                }
            ),
            linearWrapParams(density, topMargin = 4)
        )

        panel.addView(sectionLabel("Text Style"), sectionMargin(density, 22))
        panel.addView(
            styleSegments(style.fontStyle, density) { selectedStyle ->
                style = SubtitleStyleOptions.normalize(
                    style.copy(fontStyle = selectedStyle)
                )
                callbacks.onSubtitleStyleChanged(style)
                show()
            },
            linearHeightParams(
                dp(PlayerShellMetrics.SUBTITLE_STYLE_SEGMENT_HEIGHT_DP, density),
                topMargin = 10
            )
        )

        panel.addView(sectionLabel("Text Color"), sectionMargin(density, 26))
        panel.addView(colorGrid(SubtitleStyleOptions.TEXT_COLOR_PRESETS, style.fontColor, density) { color ->
            style = SubtitleStyleOptions.normalize(style.copy(fontColor = color))
            callbacks.onSubtitleStyleChanged(style)
            show()
        }, gridParams(density))

        panel.addView(sectionLabel("Background"), sectionMargin(density, 26))
        panel.addView(noBackgroundButton(style.hasNoBackground, density) {
            style = SubtitleStyleOptions.normalize(style.copy(hasNoBackground = !style.hasNoBackground))
            callbacks.onSubtitleStyleChanged(style)
            show()
        }, linearWrapParams(density, topMargin = 10))
        panel.addView(colorGrid(
            colors = SubtitleStyleOptions.BACKGROUND_COLOR_PRESETS,
            selectedColor = style.backgroundColor,
            density = density,
            enabled = !style.hasNoBackground
        ) { color ->
            style = SubtitleStyleOptions.normalize(
                style.copy(backgroundColor = color, hasNoBackground = false)
            )
            callbacks.onSubtitleStyleChanged(style)
            show()
        }, gridParams(density))

        panel.addView(sectionLabel("Bottom Position"), sectionMargin(density, 26))
        panel.addSlider(
            currentValue = style.bottomPadding,
            minimum = SubtitleStyleOptions.MIN_PERCENT,
            maximum = SubtitleStyleOptions.MAX_PERCENT,
            suffix = "px",
            density = density
        ) { value ->
            style = SubtitleStyleOptions.normalize(style.copy(bottomPadding = value))
            callbacks.onSubtitleStyleChanged(style)
        }

        panel.addView(sectionLabel("Text Shadow"), sectionMargin(density, 22))
        panel.addSlider(
            currentValue = style.shadow,
            minimum = SubtitleStyleOptions.MIN_PERCENT,
            maximum = SubtitleStyleOptions.MAX_PERCENT,
            suffix = "%",
            density = density
        ) { value ->
            style = SubtitleStyleOptions.normalize(style.copy(shadow = value))
            callbacks.onSubtitleStyleChanged(style)
        }

        panel.addView(sectionLabel("Background Opacity"), sectionMargin(density, 22))
        panel.addSlider(
            currentValue = style.backgroundOpacity,
            minimum = SubtitleStyleOptions.MIN_PERCENT,
            maximum = SubtitleStyleOptions.MAX_PERCENT,
            suffix = "%",
            density = density
        ) { value ->
            style = SubtitleStyleOptions.normalize(style.copy(backgroundOpacity = value))
            callbacks.onSubtitleStyleChanged(style)
        }

        panel.addView(sectionLabel("Custom Font"), sectionMargin(density, 26))
        panel.addActionButton("Choose font (.ttf/.otf)", true, density) {
            callbacks.onCustomFontClicked()
        }
        panel.addActionButton(
            "Clear custom font",
            style.customFontPath.isNotBlank(),
            density
        ) {
            callbacks.onClearCustomFontClicked()
        }
        panel.addResetButton(density) {
            style = SubtitleStyleOptions.normalize(SubtitleStyle())
            callbacks.onSubtitleStyleChanged(style)
            show()
        }

        scroll.addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, sidePanelParams(density))

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
                callbacks.onSubtitleStyleDismissed()
            }
        }
        popup = shownPopup
        root.alpha = 0f
        scroll.translationX = dp(PlayerShellMetrics.SUBTITLE_STYLE_PANEL_WIDTH_DP, density).toFloat()
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

    private fun title(text: String): TextView {
        return TextView(activity).apply {
            this.text = text
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun sectionLabel(text: String): TextView {
        return TextView(activity).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
    }

    private fun styleSegments(selectedStyle: Int, density: Float, onSelected: (Int) -> Unit): LinearLayout {
        val labels = arrayOf("Normal", "Bold", "Italic", "Bold+Italic")
        val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedStyle
            val chip = TextView(activity).apply {
                text = label
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) Color.WHITE else PlayerShellMetrics.MENU_TEXT_COLOR)
                background = GradientDrawable().apply {
                    setColor(if (isSelected) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_SURFACE_COLOR)
                    cornerRadius = dp(10, density).toFloat()
                }
                isFocusable = true
                setOnFocusChangeListener { view, hasFocus -> view.foreground = if (hasFocus) focusOutline(density) else null }
                setOnClickListener { onSelected(index) }
            }
            row.addView(chip, segmentParams(index, density))
        }
        return row
    }

    private fun colorGrid(colors: IntArray, selectedColor: Int, density: Float, enabled: Boolean = true, onSelected: (Int) -> Unit): GridLayout {
        val grid = GridLayout(activity).apply { columnCount = 6 }
        colors.forEach { color ->
            val isSelected = enabled && color == selectedColor
            val swatch = FrameLayout(activity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(
                        dp(if (isSelected) 3 else 1, density),
                        if (isSelected) PlayerShellMetrics.ACCENT_COLOR else Color.argb(70, 255, 255, 255)
                    )
                }
                alpha = if (enabled) 1f else 0.35f
                isEnabled = enabled
                isFocusable = enabled
                if (isSelected) {
                    addView(checkLabel(color), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                }
                setOnClickListener { if (isEnabled) onSelected(color) }
            }
            grid.addView(swatch, swatchParams(density))
        }
        return grid
    }

    private fun noBackgroundButton(isActive: Boolean, density: Float, onClick: () -> Unit): TextView {
        return TextView(activity).apply {
            text = if (isActive) "✓  No background" else "No background"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (isActive) Color.WHITE else PlayerShellMetrics.MENU_TEXT_COLOR)
            setPadding(dp(16, density), 0, dp(16, density), 0)
            background = GradientDrawable().apply {
                setColor(if (isActive) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_SURFACE_COLOR)
                cornerRadius = dp(20, density).toFloat()
            }
            isFocusable = true
            setOnFocusChangeListener { view, hasFocus -> view.foreground = if (hasFocus) focusOutline(density) else null }
            setOnClickListener { onClick() }
        }
    }

    private fun LinearLayout.addSlider(currentValue: Int, minimum: Int, maximum: Int, suffix: String, density: Float, onChanged: (Int) -> Unit) {
        val valueView = TextView(activity).apply {
            text = "$currentValue$suffix"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(PlayerShellMetrics.ACCENT_COLOR)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        addView(
            sliderRow(valueView, currentValue, minimum, maximum, suffix, density) { value ->
                valueView.text = "$value$suffix"
                onChanged(value)
            },
            linearWrapParams(density)
        )
    }

    private fun sliderRow(valueView: TextView, initialValue: Int, minimum: Int, maximum: Int, suffix: String, density: Float, onChanged: (Int) -> Unit): LinearLayout {
        val top = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(valueView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val seekBar = SeekBar(activity).apply {
            max = maximum - minimum
            progress = (initialValue - minimum).coerceIn(0, max)
            isFocusable = true
            isFocusableInTouchMode = false
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    onChanged(minimum + progress)
                }

                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(top, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(26, density)))
            addView(seekBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40, density)))
        }
    }

    private fun LinearLayout.addActionButton(label: String, enabled: Boolean, density: Float, onClick: () -> Unit) {
        val button = TextView(activity).apply {
            text = label
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
            alpha = if (enabled) 1f else 0.35f
            isEnabled = enabled
            isFocusable = enabled
            setOnFocusChangeListener { view, hasFocus ->
                view.foreground = if (hasFocus && enabled) focusOutline(density) else null
            }
            setOnClickListener { onClick() }
        }
        addView(button, linearHeightParams(dp(46, density), topMargin = 10))
    }

    private fun LinearLayout.addResetButton(density: Float, onClick: () -> Unit) {
        val button = TextView(activity).apply {
            text = "Reset to Default"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
            isFocusable = true
            setOnFocusChangeListener { view, hasFocus -> view.foreground = if (hasFocus) focusOutline(density) else null }
            setOnClickListener { onClick() }
        }
        addView(button, linearHeightParams(dp(46, density), topMargin = 34))
    }

    private fun checkLabel(color: Int): TextView {
        return TextView(activity).apply {
            text = "✓"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(if (color.isDark()) Color.WHITE else Color.BLACK)
        }
    }

    private fun focusOutline(density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(100, density).toFloat()
            setStroke(dp(2, density), Color.WHITE)
        }
    }

    private fun titleParams(density: Float) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp(PlayerShellMetrics.SUBTITLE_TITLE_HEIGHT_DP, density)
    )

    private fun sectionMargin(density: Float, topMargin: Int) = linearWrapParams(density, topMargin)

    private fun linearWrapParams(density: Float, topMargin: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(topMargin, density), 0, 0)
        }
    }

    private fun linearHeightParams(heightPx: Int, topMargin: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightPx).apply {
            setMargins(0, topMargin, 0, 0)
        }
    }

    private fun segmentParams(index: Int, density: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            setMargins(if (index == 0) 0 else dp(6, density), 0, 0, 0)
        }
    }

    private fun gridParams(density: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(10, density), 0, 0)
        }
    }

    private fun swatchParams(density: Float): GridLayout.LayoutParams {
        val size = dp(PlayerShellMetrics.SUBTITLE_STYLE_SWATCH_SIZE_DP, density)
        return GridLayout.LayoutParams().apply {
            width = size
            height = size
            setMargins(dp(6, density), dp(6, density), dp(6, density), dp(6, density))
        }
    }

    private fun sidePanelParams(density: Float) = FrameLayout.LayoutParams(
        dp(PlayerShellMetrics.SUBTITLE_STYLE_PANEL_WIDTH_DP, density),
        FrameLayout.LayoutParams.MATCH_PARENT,
        Gravity.RIGHT
    )

    private fun Int.isDark(): Boolean {
        val red = Color.red(this)
        val green = Color.green(this)
        val blue = Color.blue(this)
        return (0.299 * red + 0.587 * green + 0.114 * blue) < 140
    }

    private fun dp(value: Int, density: Float): Int = (value * density).toInt()
}
