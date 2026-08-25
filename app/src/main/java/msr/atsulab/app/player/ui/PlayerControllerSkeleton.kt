package msr.atsulab.app.player.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
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
        fun onLockClicked()
        fun onUnlockClicked()
        fun onSpeedClicked()
        fun onQualityClicked()
        fun onServerClicked()
        fun onAudioClicked()
        fun onSubtitleClicked()
        fun onCastClicked()
        fun onEpisodeClicked()
        fun onSettingsClicked()
        fun onVolumeClicked()
        fun onRewindClicked()
        fun onForwardClicked()
        fun onRotateClicked()
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

    lateinit var lockButton: ImageView
        private set

    var speedButton: ImageView? = null
        private set

    var qualityButton: LinearLayout? = null
        private set

    var serverPill: TextView? = null
        private set

    var volumeButton: ImageView? = null
        private set

    var rotateButton: ImageView? = null
        private set

    var isControlsLocked: Boolean = false
        private set

    private var statusRetryVisible = false
    private var transportVisible = false
    private var controlsVisible = false
    private val unlockHandler = Handler(Looper.getMainLooper())
    private val hideUnlockRunnable = Runnable {
        unlockButton.visibility = View.GONE
    }

    private lateinit var seekBar: SeekBar
    lateinit var skipMarkerView: PlayerSkipMarkerView
        private set
    private lateinit var currentTimeView: TextView
    private lateinit var totalTimeView: TextView
    private var userSeeking = false
    private var lastDurationMs = 0L

    val topBar: LinearLayout = createTopBar()
    val statusControls: LinearLayout = createStatusControls()
    val transportControls: LinearLayout = createTransportControls()
    val unlockButton: ImageView = createUnlockButton()

    fun setStatus(message: String, showRetry: Boolean) {
        statusRetryVisible = showRetry
        statusView.text = message
        retryButton.visibility = if (showRetry) View.VISIBLE else View.GONE
        statusControls.visibility = if (showRetry && !isControlsLocked) View.VISIBLE else View.GONE
    }

    fun setLocked(locked: Boolean) {
        if (isControlsLocked == locked) return
        isControlsLocked = locked

        if (locked) {
            topBar.visibility = View.GONE
            statusControls.visibility = View.GONE
            transportControls.visibility = View.GONE
            showUnlockOverlayTemporarily()
            return
        }

        unlockHandler.removeCallbacks(hideUnlockRunnable)
        unlockButton.animate().cancel()
        unlockButton.visibility = View.GONE
        unlockButton.alpha = 1f
        unlockButton.scaleX = 1f
        unlockButton.scaleY = 1f

        val restoreChrome = transportVisible && controlsVisible
        topBar.visibility = if (restoreChrome) View.VISIBLE else View.GONE
        statusControls.visibility = if (statusRetryVisible) View.VISIBLE else View.GONE
        transportControls.visibility = if (restoreChrome) View.VISIBLE else View.GONE
    }

    fun release() {
        unlockHandler.removeCallbacks(hideUnlockRunnable)
        unlockButton.animate().cancel()
    }

    fun showUnlockOverlayTemporarily() {
        if (!isControlsLocked) return
        unlockHandler.removeCallbacks(hideUnlockRunnable)
        unlockButton.animate().cancel()
        unlockButton.alpha = 1f
        unlockButton.scaleX = 1f
        unlockButton.scaleY = 1f
        unlockButton.visibility = View.VISIBLE
        unlockHandler.postDelayed(hideUnlockRunnable, UNLOCK_AUTO_HIDE_DELAY_MS)
    }

    fun setTransportState(
        isVisible: Boolean,
        controlsShown: Boolean,
        isPlaying: Boolean,
        canShowPrevious: Boolean,
        canShowNext: Boolean
    ) {
        transportVisible = isVisible
        controlsVisible = controlsShown
        val effectiveVisible = isVisible && controlsShown && !isControlsLocked
        topBar.visibility = if (effectiveVisible) View.VISIBLE else View.GONE
        transportControls.visibility = if (effectiveVisible) View.VISIBLE else View.GONE
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

            val backButton = createPlayerIcon(R.drawable.ic_back) {
                callbacks.onBackClicked()
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

            if (isLandscape) {
                val pill = TextView(context).apply {
                    text = "S1"
                    textSize = 14f
                    minHeight = 0
                    gravity = Gravity.CENTER
                    setTextColor(PlayerShellMetrics.MENU_TEXT_COLOR)
                    background = GradientDrawable().apply {
                        setColor(PlayerShellMetrics.SURFACE_COLOR)
                        cornerRadius = dp(100, density).toFloat()
                        setStroke(dp(1, density), 0x460EA5E9.toInt())
                    }
                    setOnClickListener { callbacks.onServerClicked() }
                }
                serverPill = pill
                addView(
                    pill,
                    LinearLayout.LayoutParams(
                        dp(PlayerShellMetrics.SERVER_PILL_WIDTH_DP, density),
                        dp(PlayerShellMetrics.SERVER_PILL_HEIGHT_DP, density)
                    )
                )

                val speedIcon = createPlayerIcon(R.drawable.ic_player_speed_modern, R.string.player_speed) {
                    callbacks.onSpeedClicked()
                }
                speedButton = speedIcon
                addView(speedIcon, topIconParams(density))

                val qualityPill = TextView(context).apply {
                    text = "AUTO"
                    textSize = 12f
                    minHeight = 0
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                }
                qualityButton = LinearLayout(context).apply {
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.ripple_circle)
                    setPadding(dp(10, density), 0, dp(10, density), 0)
                    addView(qualityPill)
                    setOnClickListener { callbacks.onQualityClicked() }
                }
                addView(
                    qualityButton,
                    LinearLayout.LayoutParams(
                        dp(54, density),
                        dp(42, density)
                    ).apply {
                        setMargins(0, 0, dp(8, density), 0)
                    }
                )
            }

            addView(
                createPlayerIcon(R.drawable.ic_player_audio_modern, R.string.player_audio) {
                    callbacks.onAudioClicked()
                },
                topIconParams(density)
            )
            addView(
                createPlayerIcon(R.drawable.ic_subtitle, R.string.player_subtitles) {
                    callbacks.onSubtitleClicked()
                },
                topIconParams(density)
            )
            addView(
                createPlayerIcon(R.drawable.ic_cast_button_static, R.string.player_cast) {
                    callbacks.onCastClicked()
                },
                topIconParams(density)
            )

            if (isLandscape) {
                addView(
                    createPlayerIcon(R.drawable.ic_episode, R.string.player_episodes) {
                        callbacks.onEpisodeClicked()
                    },
                    topIconParams(density)
                )
                addView(
                    createPlayerIcon(R.drawable.ic_settings, R.string.player_settings) {
                        callbacks.onSettingsClicked()
                    },
                    topIconParams(density)
                )
            }
        }
    }

    private fun topIconParams(density: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(dp(42, density), dp(42, density)).apply {
            setMargins(0, 0, dp(8, density), 0)
        }
    }

    private fun createPlayerIcon(
        resourceId: Int,
        contentDescriptionResId: Int,
        onClick: () -> Unit
    ): ImageView = createPlayerIcon(resourceId, context.getString(contentDescriptionResId), onClick)

    private fun createPlayerIcon(resourceId: Int, onClick: () -> Unit): ImageView =
        createPlayerIcon(resourceId, null, onClick)

    private fun createPlayerIcon(resourceId: Int, description: String?, onClick: () -> Unit): ImageView {
        val density = context.resources.displayMetrics.density
        return ImageView(context).apply {
            setImageResource(resourceId)
            if (description != null) contentDescription = description
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8, density), dp(8, density), dp(8, density), dp(8, density))
            setBackgroundResource(R.drawable.ripple_circle)
            setOnClickListener { onClick() }
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
        val isLandscape = orientation == PlayerShellOrientation.LANDSCAPE

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

            skipMarkerView = PlayerSkipMarkerView(context).apply {
                visibility = View.GONE
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            val seekBarHost = FrameLayout(context).apply {
                addView(
                    skipMarkerView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                addView(
                    seekBar,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
            progressRow.addView(
                seekBarHost,
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
            lockButton = createTransportIcon(
                R.drawable.ic_unlock,
                R.string.player_lock_controls
            ) {
                callbacks.onLockClicked()
            }
            iconRow.addView(
                lockButton,
                FrameLayout.LayoutParams(
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    Gravity.LEFT or Gravity.CENTER_VERTICAL
                ).apply {
                    setMargins(dp(PlayerShellMetrics.LOCK_BUTTON_LEFT_MARGIN_DP, density), 0, 0, 0)
                }
            )

            val volumeButton = createPlayerIcon(R.drawable.ic_volume_up, R.string.player_volume) {
                callbacks.onVolumeClicked()
            }
            this@PlayerControllerSkeleton.volumeButton = volumeButton
            iconRow.addView(
                volumeButton,
                FrameLayout.LayoutParams(
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    Gravity.LEFT or Gravity.CENTER_VERTICAL
                ).apply {
                    setMargins(dp(PlayerShellMetrics.VOLUME_BUTTON_LEFT_MARGIN_DP, density), 0, 0, 0)
                }
            )

            if (isLandscape) {
                iconRow.addView(
                    createPlayerIcon(R.drawable.ic_player_rewind_modern, R.string.player_rewind) {
                        callbacks.onRewindClicked()
                    },
                    FrameLayout.LayoutParams(
                        dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                        dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                        Gravity.CENTER
                    ).apply {
                        setMargins(0, 0, dp(PlayerShellMetrics.SEEK_BUTTON_OFFSET_DP, density), 0)
                    }
                )
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

            if (isLandscape) {
                iconRow.addView(
                    createPlayerIcon(R.drawable.ic_player_forward_modern, R.string.player_forward) {
                        callbacks.onForwardClicked()
                    },
                    FrameLayout.LayoutParams(
                        dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                        dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                        Gravity.CENTER
                    ).apply {
                        setMargins(dp(PlayerShellMetrics.SEEK_BUTTON_OFFSET_DP, density), 0, 0, 0)
                    }
                )
            }

            val rotateButton = createPlayerIcon(
                if (isLandscape) {
                    R.drawable.ic_player_fullscreen_exit_modern
                } else {
                    R.drawable.ic_player_fullscreen_enter_modern
                },
                R.string.player_rotate
            ) {
                callbacks.onRotateClicked()
            }
            this@PlayerControllerSkeleton.rotateButton = rotateButton
            iconRow.addView(
                rotateButton,
                FrameLayout.LayoutParams(
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    dp(PlayerShellMetrics.CONTROL_ICON_SIZE_DP, density),
                    Gravity.RIGHT or Gravity.CENTER_VERTICAL
                ).apply {
                    setMargins(0, 0, dp(PlayerShellMetrics.ROTATE_BUTTON_RIGHT_MARGIN_DP, density), 0)
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

    fun updateServerLabel(label: String) {
        serverPill?.text = label
    }

    fun updateQualityLabel(label: String) {
        (qualityButton?.getChildAt(0) as? TextView)?.text = label
    }

    fun setVolumeMuted(isMuted: Boolean) {
        volumeButton?.setImageResource(
            if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
        )
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

    private fun createUnlockButton(): ImageView {
        val density = context.resources.displayMetrics.density

        return ImageView(context).apply {
            setImageResource(R.drawable.ic_unlock)
            contentDescription = context.getString(R.string.player_unlock_controls)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8, density), dp(8, density), dp(8, density), dp(8, density))
            setBackgroundResource(R.drawable.ripple_circle)
            visibility = View.GONE
            setOnClickListener {
                callbacks.onUnlockClicked()
            }
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
        const val UNLOCK_AUTO_HIDE_DELAY_MS = 2_000L
    }
}
