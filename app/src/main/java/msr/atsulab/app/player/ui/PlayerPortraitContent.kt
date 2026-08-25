package msr.atsulab.app.player.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import msr.atsulab.app.R
import msr.atsulab.app.player.domain.model.PlaybackEpisode
import msr.atsulab.app.player.domain.model.VideoSource

internal data class PlayerPortraitUiState(
    val episodes: List<PlaybackEpisode> = emptyList(),
    val currentEpisode: PlaybackEpisode? = null,
    val rangeStart: Int = 0,
    val showDub: Boolean = false,
    val sources: List<VideoSource> = emptyList(),
    val selectedSourceIndex: Int = -1,
    val isLoadingSources: Boolean = true,
    val isMoreServersLoading: Boolean = false,
    val hasAllServersFailed: Boolean = false
)

internal class PlayerPortraitContent(
    private val context: Context,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun currentState(): PlayerPortraitUiState?
        fun onLanguageModeSelected(showDub: Boolean)
        fun onServerSelected(sourceIndex: Int)
        fun onEpisodeSelected(episode: PlaybackEpisode)
        fun onRangeSelected(rangeStart: Int)
    }

    val view: View = buildView()

    private val density = context.resources.displayMetrics.density
    private lateinit var subTab: TextView
    private lateinit var dubTab: TextView
    private lateinit var languageIndicator: View
    private lateinit var serverRow: LinearLayout
    private lateinit var serverScroll: HorizontalScrollView
    private lateinit var serverOverflowHint: ImageView
    private lateinit var serverOverflowFade: View
    private lateinit var sourceErrorView: TextView
    private lateinit var rangeView: TextView
    private lateinit var episodeGrid: GridLayout
    private lateinit var searchInput: EditText
    private var rangePopup: PopupWindow? = null

    fun render(state: PlayerPortraitUiState) {
        updateLanguage(state.showDub)
        renderServers(state)
        renderEpisodes(state)
    }

    fun setSourceError(message: String?) {
        sourceErrorView.text = message.orEmpty()
        sourceErrorView.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    fun dismissPopups() {
        rangePopup?.dismiss()
        rangePopup = null
    }

    private fun buildView(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(buildLanguageTabs())
            addView(buildServerSection())
            addView(buildSourceError())
            addView(buildEpisodeTitle())
            addView(buildEpisodeTools())
            episodeGrid = buildEpisodeGrid()
            episodeGrid.layoutParams = LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(18)
            }
            addView(episodeGrid)
            addView(buildCommentPreview())
        }
    }

    private fun buildLanguageTabs(): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val tabs = LinearLayout(context).apply { gravity = Gravity.CENTER }
        subTab = languageTab { callbacks.onLanguageModeSelected(false) }
        dubTab = languageTab { callbacks.onLanguageModeSelected(true) }
        tabs.addView(subTab, LinearLayout.LayoutParams(0, dp(46), 1f))
        tabs.addView(dubTab, LinearLayout.LayoutParams(0, dp(46), 1f))

        val indicator = FrameLayout(context)
        val background = View(context).apply {
            setBackgroundColor(PlayerShellMetrics.MENU_BORDER_COLOR)
        }
        languageIndicator = View(context).apply {
            setBackgroundColor(PlayerShellMetrics.ACCENT_COLOR)
        }
        indicator.addView(
            background,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(2), Gravity.BOTTOM)
        )
        indicator.addView(languageIndicator, FrameLayout.LayoutParams(dp(80), dp(4), Gravity.BOTTOM or Gravity.START))
        container.addView(tabs, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(46)))
        container.addView(indicator, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(4)))
        return container
    }

    private fun languageTab(onClick: () -> Unit): TextView {
        return TextView(context).apply {
            gravity = Gravity.CENTER
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { onClick() }
        }
    }

    private fun updateLanguage(showDub: Boolean) {
        val selectedColor = Color.WHITE
        val unselectedColor = PlayerShellMetrics.MENU_TEXT_COLOR
        subTab.setTextColor(if (showDub) unselectedColor else selectedColor)
        dubTab.setTextColor(if (showDub) selectedColor else unselectedColor)
        languageIndicator.post {
            val parentView = languageIndicator.parent as? View
            val halfWidth = maxOf(0, (parentView?.width ?: 0) / 2)
            languageIndicator.layoutParams = languageIndicator.layoutParams.apply {
                width = halfWidth
            }
            languageIndicator.translationX = if (showDub) halfWidth.toFloat() else 0f
            languageIndicator.requestLayout()
        }
    }

    private fun buildServerSection(): View {
        serverRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        serverScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(serverRow, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val wrapper = FrameLayout(context)
        wrapper.addView(serverScroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        serverOverflowFade = View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, Color.argb(170, 0, 0, 0))
            )
            visibility = View.GONE
        }
        wrapper.addView(
            serverOverflowFade,
            FrameLayout.LayoutParams(dp(34), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT)
        )

        serverOverflowHint = ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron_down)
            rotation = -90f
            setColorFilter(Color.WHITE)
            alpha = 0.95f
            setPadding(dp(6), dp(6), dp(6), dp(6))
            visibility = View.GONE
        }
        wrapper.addView(
            serverOverflowHint,
            FrameLayout.LayoutParams(dp(24), dp(24), Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply {
                rightMargin = dp(4)
            }
        )

        serverScroll.viewTreeObserver.addOnGlobalLayoutListener(::updateServerOverflow)
        serverScroll.setOnScrollChangeListener { _, _, _, _, _ -> updateServerOverflow() }
        serverRow.viewTreeObserver.addOnGlobalLayoutListener(::updateServerOverflow)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(wrapper, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(58)).apply {
                topMargin = dp(8)
            })
        }
    }

    private fun updateServerOverflow() {
        val canScrollRight = serverRow.width > serverScroll.width &&
            serverScroll.scrollX + serverScroll.width < serverRow.width - dp(8)
        serverOverflowFade.visibility = if (canScrollRight) View.VISIBLE else View.GONE
        serverOverflowHint.visibility = if (canScrollRight) View.VISIBLE else View.GONE
    }

    private fun renderServers(state: PlayerPortraitUiState) {
        serverRow.removeAllViews()
        serverRow.gravity = Gravity.CENTER_VERTICAL

        if (state.isLoadingSources && state.sources.isEmpty()) {
            serverRow.gravity = Gravity.CENTER
            serverRow.addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(6), 0, dp(6))
                    addView(ProgressBar(context).apply {
                        isIndeterminate = true
                    }, LinearLayout.LayoutParams(dp(18), dp(18)))
                    addView(TextView(context).apply {
                        text = context.getString(R.string.player_loading_sources)
                        textSize = 13f
                        setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                        setPadding(dp(10), 0, 0, 0)
                    })
                },
                LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(36))
            )
            updateServerOverflow()
            return
        }

        PlayerServerMenuModel.options(
            sources = state.sources,
            selectedIndex = state.selectedSourceIndex,
            showDub = state.showDub
        ).forEach { option ->
            val chip = if (option.isSelected) {
                TextView(context).apply {
                    text = option.label
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        setColor(PlayerShellMetrics.ACCENT_COLOR)
                        cornerRadius = dp(100).toFloat()
                    }
                }
            } else {
                TextView(context).apply {
                    text = option.label
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                    background = GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        cornerRadius = dp(100).toFloat()
                        setStroke(dp(2), PlayerShellMetrics.MENU_BORDER_COLOR)
                    }
                    setOnClickListener { callbacks.onServerSelected(option.sourceIndex) }
                }
            }
            chip.minimumWidth = dp(64)
            chip.minHeight = dp(36)
            chip.setPadding(dp(16), dp(8), dp(16), dp(8))
            serverRow.addView(chip, linearParams(height = dp(36)))
        }

        if (state.isMoreServersLoading) {
            serverRow.addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ProgressBar(context).apply { isIndeterminate = true }, LinearLayout.LayoutParams(dp(16), dp(16)))
                    addView(TextView(context).apply {
                        text = context.getString(R.string.player_loading_more_servers)
                        textSize = 12f
                        setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                        setPadding(dp(6), 0, 0, 0)
                    })
                },
                LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply {
                    leftMargin = dp(4)
                }
            )
        }

        if (state.sources.isEmpty() && state.hasAllServersFailed) {
            serverRow.addView(TextView(context).apply {
                text = context.getString(R.string.player_no_sources)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                alpha = 0.55f
            }, linearParams(height = dp(36)))
        }
        updateServerOverflow()
    }

    private fun buildSourceError(): TextView {
        sourceErrorView = TextView(context).apply {
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF5470"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            visibility = View.GONE
        }
        return sourceErrorView
    }

    private fun buildEpisodeTitle(): TextView {
        return TextView(context).apply {
            text = context.getString(R.string.player_list_of_episodes)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(26)
                bottomMargin = dp(18)
            }
        }
    }

    private fun buildEpisodeTools(): View {
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }

        val listIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_format_list_bulleted)
            setColorFilter(PlayerShellMetrics.ACCENT_COLOR)
            setOnClickListener { showRangePopup(this) }
        }
        row.addView(listIcon, FrameLayout.LayoutParams(dp(32), dp(32)))

        rangeView = TextView(context).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(PlayerShellMetrics.ACCENT_COLOR)
            setOnClickListener { anchor -> showRangePopup(anchor) }
        }
        row.addView(rangeView, LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = dp(18)
        })

        val arrow = ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron_down)
            setColorFilter(PlayerShellMetrics.ACCENT_COLOR)
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener { anchor -> showRangePopup(anchor) }
        }
        row.addView(arrow, FrameLayout.LayoutParams(dp(34), dp(34)))

        val search = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
                cornerRadius = dp(27).toFloat()
                setStroke(dp(1), PlayerShellMetrics.MENU_BORDER_COLOR)
            }
        }
        val searchIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            setColorFilter(PlayerShellMetrics.MENU_TEXT_COLOR)
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener { searchFromInput() }
        }
        search.addView(searchIcon, FrameLayout.LayoutParams(dp(36), dp(36), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            leftMargin = dp(12)
        })

        searchInput = EditText(context).apply {
            hint = context.getString(R.string.player_number_of_episode)
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
            textSize = 18f
            setBackgroundColor(Color.TRANSPARENT)
            inputType = InputType.TYPE_CLASS_NUMBER
            setImeActionLabel(context.getString(R.string.search), android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)
            setOnEditorActionListener { _, _, _ ->
                searchFromInput()
                true
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(text: Editable?) {
                    if (text?.length?.compareTo(2) == 0 || text?.length == 3) searchFromInput()
                }
            })
        }
        search.addView(searchInput, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            leftMargin = dp(60)
            rightMargin = dp(12)
        })
        row.addView(search, LinearLayout.LayoutParams(0, dp(54), 1.6f))
        return row
    }

    private fun buildEpisodeGrid(): GridLayout {
        return GridLayout(context).apply {
            columnCount = GRID_COLUMN_COUNT
        }
    }

    private fun renderEpisodes(state: PlayerPortraitUiState) {
        val ranges = PlayerEpisodePanelModel.ranges(state.episodes, state.rangeStart)
        rangeView.text = PlayerEpisodePanelModel.controlLabel(ranges)
        episodeGrid.removeAllViews()

        val cellMargin = dp(4)
        val availableWidth = context.resources.displayMetrics.widthPixels - dp(74)
        val cellWidth = maxOf(dp(40), availableWidth / GRID_COLUMN_COUNT - cellMargin * 2)
        val options = PlayerEpisodePanelModel.gridOptions(
            episodes = state.episodes,
            currentEpisode = state.currentEpisode,
            selectedRangeStart = state.rangeStart
        )

        if (options.isEmpty()) {
            episodeGrid.addView(TextView(context).apply {
                text = context.getString(R.string.player_no_episodes_in_range)
                textSize = 15f
                setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
            }, GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.MATCH_PARENT
                height = FrameLayout.LayoutParams.WRAP_CONTENT
            })
            return
        }

        options.forEach { option ->
            val episodeView = TextView(context).apply {
                text = option.number.toString()
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(if (option.isSelected) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_BORDER_COLOR)
                    cornerRadius = dp(10).toFloat()
                }
                setOnClickListener { callbacks.onEpisodeSelected(option.episode) }
            }
            episodeGrid.addView(episodeView, GridLayout.LayoutParams().apply {
                width = cellWidth
                height = dp(58)
                setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
            })
        }
    }

    private fun buildCommentPreview(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(PlayerShellMetrics.MENU_BORDER_COLOR)
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(20), dp(18), dp(20), dp(18))
            layoutParams = LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(126)).apply {
                topMargin = dp(28)
            }
            addView(TextView(context).apply {
                text = context.getString(R.string.player_comments_title)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            })
            addView(TextView(context).apply {
                text = context.getString(R.string.player_comments_preview)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    private fun showRangePopup(anchor: View) {
        dismissRangePopup()
        val state = currentUiState() ?: return
        val popupScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, dp(8), 0, dp(8))
            background = GradientDrawable().apply {
                setColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
                cornerRadius = dp(16).toFloat()
            }
        }
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val ranges = PlayerEpisodePanelModel.ranges(state.episodes, state.rangeStart)
        ranges.forEach { range ->
            val row = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(10), dp(14), dp(10))
                if (range.isSelected) {
                    background = GradientDrawable().apply {
                        setColor(PlayerShellMetrics.MENU_SURFACE_COLOR)
                        cornerRadius = dp(10).toFloat()
                    }
                }
                setOnClickListener {
                    callbacks.onRangeSelected(range.start)
                    dismissRangePopup()
                }
            }
            row.addView(TextView(context).apply {
                text = "EPS ${range.start} - ${range.endInclusive}"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (range.isSelected) Color.WHITE else PlayerShellMetrics.MENU_TEXT_COLOR)
            }, LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(context).apply {
                text = "${range.endInclusive - range.start + 1} eps"
                textSize = 11f
                setTextColor(if (range.isSelected) PlayerShellMetrics.ACCENT_COLOR else PlayerShellMetrics.MENU_TEXT_COLOR)
                gravity = Gravity.CENTER_VERTICAL
            })
            list.addView(row, LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(8), dp(4), dp(8), dp(4))
            })
        }
        popupScroll.addView(list)
        val maxHeight = minOf(dp(420), context.resources.displayMetrics.heightPixels / 2)
        val popup = PopupWindow(popupScroll, dp(280), minOf(maxHeight, ranges.size * dp(56) + dp(16)), true).apply {
            isOutsideTouchable = true
            elevation = dp(16).toFloat()
            setOnDismissListener { rangePopup = null }
        }
        rangePopup = popup
        popup.showAsDropDown(anchor, 0, dp(8))
    }

    private fun searchFromInput() {
        val requestedNumber = searchInput.text?.toString()?.trim()?.toIntOrNull() ?: return
        val episode = currentUiState()?.episodes?.firstOrNull {
            it.number.toInt() == requestedNumber
        } ?: return
        callbacks.onEpisodeSelected(episode)
    }

    private fun currentUiState(): PlayerPortraitUiState? = callbacks.currentState()

    private fun dismissRangePopup() {
        rangePopup?.dismiss()
        rangePopup = null
    }

    private fun linearParams(width: Int = FrameLayout.LayoutParams.WRAP_CONTENT, height: Int = FrameLayout.LayoutParams.WRAP_CONTENT): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(width, height).apply {
            rightMargin = dp(8)
        }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        const val GRID_COLUMN_COUNT = 5
    }
}
