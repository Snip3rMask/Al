package msr.atsulab.app.player.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
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
        fun onPlayPauseClicked()
        fun onPreviousEpisodeClicked()
        fun onNextEpisodeClicked()
        fun onSeekFinished(fraction: Float)
    }

    lateinit var titleView: TextView
        private set

    lateinit var statusView: TextView
        private set

    lateinit var retryButton: MaterialButton
        private set

    lateinit var previousEpisodeButton: ImageView
        private set

    lateinit var playPauseButton: ImageView
        private set

    lateinit var nextEpisodeButton: ImageView
        private set

    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeView: TextView
    private lateinit var totalTimeView: TextView
    private var userSeeking = false
    private var lastDurationMs = 0L

    val topBar: LinearLayout = createTopBar()
    val statusControls: LinearLayout = createStatusControls()
    val transportControls: LinearLayout = createTransportControls()

    fun setStatus(message: String, showRetry: Boolean) {
        statusView.text = message
        statusControls.visibility = if (showRetry) View.VISIBLE else View.GONE
        retryButton.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    fun setTransportState(
        isVisible: Boolean,
        isPlaying: Boolean,
        canShowPrevious: Boolean,
        canShowNext: Boolean
    ) {
        transportControls.visibility = if (isVisible) View.VISIBLE else View.GONE
        playPauseButton.setImageResource(
            if (isPlaying) R.drawable.ic_player_pause_modern else R.drawable.ic_player_play_modern
        )
        playPauseButton.contentDescription = context.getString(if (isPlaying) R.string.pause else R.string.play)

        previousEpisodeButton.isEnabled = canShowPrevious
        previousEpisodeButton.alpha = if (canShowPrevious) 1f else 0.38f
        nextEpisodeButton.isEnabled = canShowNext
        nextEpisodeButton.alpha = if (canShowNext) 1f else 0.38f
    }

    fun updatePlaybackProgress(positionMs: Long, bufferedPositionMs: Long, durationMs: Long) {
        lastDurationMs = durationMs
        currentTimeView.text = PlayerTimeFormatter.format(positionMs)
        totalTimeView.text = PlayerTimeFormatter.format(durationMs)
        seekBar.isEnabled = durationMs > 0L
        if (userSeeking || durationMs <= 0L) return

        seekBar.progress = progressFraction(positionMs, durationMs)
        seekBar.secondaryProgress = maxOf(
            seekBar.progress,
            progressFraction(bufferedPositionMs, durationMs)
        )
    }

    private fun progressFraction(positionMs: Long, durationMs: Long): Int {
        if (durationMs <= 0L) return 0
        return (positionMs.coerceAtLeast(0L) * SEEK_BAR_MAX / durationMs).toInt().coerceIn(0, SEEK_BAR_MAX)
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

    private fun createTransportControls(): LinearLayout {
        val density = context.resources.displayMetrics.density

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            background = createBottomGradient()

            val progressRow = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(30, density), 0, dp(30, density), 0)
            }

            currentTimeView = TextView(context).apply {
                text = PlayerTimeFormatter.format(0L)
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            progressRow.addView(
                currentTimeView,
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            )

            seekBar = SeekBar(context).apply {
                max = SEEK_BAR_MAX
                progressDrawable = createBufferedSeekDrawable(density)
                thumb = createThinSeekThumb(density)
                thumbOffset = dp(6, density)
                splitTrack = false
                isEnabled = false
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser || lastDurationMs <= 0L) return
                        val previewPositionMs = lastDurationMs * progress / SEEK_BAR_MAX
                        currentTimeView.text = PlayerTimeFormatter.format(previewPositionMs)
                    }

                    override fun onStartTrackingTouch(bar: SeekBar?) {
                        userSeeking = true
                    }

                    override fun onStopTrackingTouch(bar: SeekBar?) {
                        userSeeking = false
                        callbacks.onSeekFinished((bar?.progress ?: 0) / SEEK_BAR_MAX.toFloat())
                    }
                })
            }
            progressRow.addView(
                seekBar,
                LinearLayout.LayoutParams(0, dp(PlayerShellMetrics.SEEK_CONTROL_HEIGHT_DP, density), 1f).apply {
                    setMargins(dp(10, density), 0, dp(10, density), 0)
                }
            )

            totalTimeView = TextView(context).apply {
                text = PlayerTimeFormatter.format(0L)
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            progressRow.addView(
                totalTimeView,
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            )

            val iconRow = FrameLayout(context)
            previousEpisodeButton = createTransportIcon(
                R.drawable.ic_player_previous_modern,
                R.string.player_previous_episode
            ) {
                callbacks.onPreviousEpisodeClicked()
            }
            playPauseButton = createTransportIcon(
                R.drawable.ic_player_play_modern,
                R.string.player_play_pause
            ) {
                callbacks.onPlayPauseClicked()
            }
            nextEpisodeButton = createTransportIcon(
                R.drawable.ic_player_next_modern,
                R.string.player_next_episode
            ) {
                callbacks.onNextEpisodeClicked()
            }
            iconRow.addView(
                previousEpisodeButton,
                FrameLayout.LayoutParams(
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    Gravity.CENTER
                ).apply {
                    setMargins(0, 0, dp(PlayerShellMetrics.TRANSPORT_ICON_OFFSET_DP, density), 0)
                }
            )
            iconRow.addView(
                playPauseButton,
                FrameLayout.LayoutParams(
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    Gravity.CENTER
                )
            )
            iconRow.addView(
                nextEpisodeButton,
                FrameLayout.LayoutParams(
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    Gravity.CENTER
                ).apply {
                    setMargins(dp(PlayerShellMetrics.TRANSPORT_ICON_OFFSET_DP, density), 0, 0, 0)
                }
            )

            addView(
                progressRow,
                LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    dp(PlayerShellMetrics.PROGRESS_ROW_HEIGHT_DP, density)
                )
            )
            addView(
                iconRow,
                LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    dp(PlayerShellMetrics.TRANSPORT_ROW_HEIGHT_DP, density)
                )
            )
        }
    }

    private fun createTransportIcon(resourceId: Int, contentDescriptionResId: Int, onClick: () -> Unit): ImageView {
        val density = context.resources.displayMetrics.density

        return ImageView(context).apply {
            setImageResource(resourceId)
            contentDescription = context.getString(contentDescriptionResId)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8, density), dp(8, density), dp(8, density), dp(8, density))
            setBackgroundResource(R.drawable.ripple_circle)
            setOnClickListener { onClick() }
        }
    }

    private fun createBufferedSeekDrawable(density: Float): LayerDrawable {
        val background = roundedLine(0x665F6672.toInt(), density)
        val secondary = ClipDrawable(roundedLine(0x99FFFFFF.toInt(), density), Gravity.LEFT, ClipDrawable.HORIZONTAL)
        val progress = ClipDrawable(
            roundedLine(PlayerShellMetrics.ACCENT_COLOR, density),
            Gravity.LEFT,
            ClipDrawable.HORIZONTAL
        )
        return LayerDrawable(arrayOf(background, secondary, progress)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.secondaryProgress)
            setId(2, android.R.id.progress)
            val inset = dp(14, density)
            setLayerInset(0, 0, inset, 0, inset)
            setLayerInset(1, 0, inset, 0, inset)
            setLayerInset(2, 0, inset, 0, inset)
        }
    }

    private fun createThinSeekThumb(density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(PlayerShellMetrics.ACCENT_COLOR)
            setSize(dp(12, density), dp(12, density))
        }
    }

    private fun roundedLine(color: Int, density: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(2, density).toFloat()
            setSize(1, dp(3, density))
        }
    }

    private fun createTopGradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xCC000000.toInt(), 0x00000000)
        )
    }

    private fun createBottomGradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(0xCC000000.toInt(), 0x66000000.toInt(), 0x00000000)
        )
    }

    private fun dp(value: Int, density: Float): Int {
        return (value * density).toInt()
    }

    private companion object {
        const val SEEK_BAR_MAX = 1000
        const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
