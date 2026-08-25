package msr.atsulab.app.player.storage

import android.content.Context
import androidx.core.content.edit
import msr.atsulab.app.player.domain.PlaybackSpeedOptions
import msr.atsulab.app.player.domain.SubtitleStyleOptions
import msr.atsulab.app.player.domain.model.SubtitleStyle

class DefaultPlaybackPreferencesStore(
    context: Context
) : PlaybackPreferencesStore {

    private val preferences by lazy {
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun getSpeed(): Float {
        return PlaybackSpeedOptions.normalize(preferences.getFloat(SPEED_KEY, PlaybackSpeedOptions.DEFAULT))
    }

    override fun setSpeed(speed: Float) {
        preferences.edit {
            putFloat(SPEED_KEY, PlaybackSpeedOptions.normalize(speed))
        }
    }

    override fun isFrameCaptureEnabled(): Boolean {
        return preferences.getBoolean(FRAME_CAPTURE_ENABLED_KEY, true)
    }

    override fun setFrameCaptureEnabled(enabled: Boolean) {
        preferences.edit {
            putBoolean(FRAME_CAPTURE_ENABLED_KEY, enabled)
        }
    }

    override fun isFrameCaptureAlwaysVisible(): Boolean {
        return preferences.getBoolean(FRAME_CAPTURE_ALWAYS_VISIBLE_KEY, false)
    }

    override fun setFrameCaptureAlwaysVisible(enabled: Boolean) {
        preferences.edit {
            putBoolean(FRAME_CAPTURE_ALWAYS_VISIBLE_KEY, enabled)
        }
    }

    override fun getFrameCapturePositionX(): Float {
        return preferences.getFloat(FRAME_CAPTURE_POSITION_X_KEY, -1f).coerceIn(-1f, 1f)
    }

    override fun getFrameCapturePositionY(): Float {
        return preferences.getFloat(FRAME_CAPTURE_POSITION_Y_KEY, -1f).coerceIn(-1f, 1f)
    }

    override fun setFrameCapturePosition(xFraction: Float, yFraction: Float) {
        preferences.edit {
            putFloat(FRAME_CAPTURE_POSITION_X_KEY, xFraction.coerceIn(0f, 1f))
            putFloat(FRAME_CAPTURE_POSITION_Y_KEY, yFraction.coerceIn(0f, 1f))
        }
    }

    override fun getFrameCaptureDirectoryUri(): String {
        return preferences.getString(FRAME_CAPTURE_DIRECTORY_KEY, "").orEmpty()
    }

    override fun setFrameCaptureDirectoryUri(uri: String) {
        preferences.edit {
            putString(FRAME_CAPTURE_DIRECTORY_KEY, uri)
        }
    }

    override fun clearFrameCapturePosition() {
        preferences.edit {
            remove(FRAME_CAPTURE_POSITION_X_KEY)
            remove(FRAME_CAPTURE_POSITION_Y_KEY)
        }
    }

    override fun getSubtitleStyle(): SubtitleStyle {
        return SubtitleStyleOptions.normalize(
            SubtitleStyle(
                fontSize = preferences.getFloat(FONT_SIZE_KEY, SubtitleStyle.DEFAULT_FONT_SIZE),
                fontColor = preferences.getInt(FONT_COLOR_KEY, SubtitleStyle.DEFAULT_FONT_COLOR),
                fontStyle = preferences.getInt(
                    FONT_STYLE_KEY,
                    SubtitleStyle.FONT_STYLE_NORMAL
                ),
                backgroundColor = preferences.getInt(
                    BACKGROUND_COLOR_KEY,
                    SubtitleStyle.DEFAULT_BACKGROUND_COLOR
                ),
                backgroundOpacity = preferences.getInt(
                    BACKGROUND_OPACITY_KEY,
                    SubtitleStyle.DEFAULT_BACKGROUND_OPACITY
                ),
                hasNoBackground = preferences.getBoolean(NO_BACKGROUND_KEY, false),
                bottomPadding = preferences.getInt(
                    BOTTOM_PADDING_KEY,
                    SubtitleStyle.DEFAULT_BOTTOM_PADDING
                ),
                shadow = preferences.getInt(SHADOW_KEY, SubtitleStyle.DEFAULT_SHADOW),
                customFontPath = preferences.getString(
                    CUSTOM_FONT_PATH_KEY,
                    SubtitleStyle().customFontPath
                ).orEmpty()
            )
        )
    }

    override fun setSubtitleStyle(style: SubtitleStyle) {
        val normalizedStyle = SubtitleStyleOptions.normalize(style)
        preferences.edit {
            putFloat(FONT_SIZE_KEY, normalizedStyle.fontSize)
            putInt(FONT_COLOR_KEY, normalizedStyle.fontColor)
            putInt(FONT_STYLE_KEY, normalizedStyle.fontStyle)
            putInt(BACKGROUND_COLOR_KEY, normalizedStyle.backgroundColor)
            putInt(BACKGROUND_OPACITY_KEY, normalizedStyle.backgroundOpacity)
            putBoolean(NO_BACKGROUND_KEY, normalizedStyle.hasNoBackground)
            putInt(BOTTOM_PADDING_KEY, normalizedStyle.bottomPadding)
            putInt(SHADOW_KEY, normalizedStyle.shadow)
            putString(CUSTOM_FONT_PATH_KEY, normalizedStyle.customFontPath)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "atsu_playback_preferences"
        const val SPEED_KEY = "playback_speed"
        const val FRAME_CAPTURE_ENABLED_KEY = "frame_capture_enabled"
        const val FRAME_CAPTURE_ALWAYS_VISIBLE_KEY = "frame_capture_always_visible"
        const val FRAME_CAPTURE_POSITION_X_KEY = "frame_capture_pos_x"
        const val FRAME_CAPTURE_POSITION_Y_KEY = "frame_capture_pos_y"
        const val FRAME_CAPTURE_DIRECTORY_KEY = "frame_dir_uri"
        const val FONT_SIZE_KEY = "subtitle_font_size"
        const val FONT_COLOR_KEY = "subtitle_font_color"
        const val FONT_STYLE_KEY = "subtitle_font_style"
        const val BACKGROUND_COLOR_KEY = "subtitle_bg_color"
        const val BACKGROUND_OPACITY_KEY = "subtitle_bg_opacity"
        const val NO_BACKGROUND_KEY = "subtitle_bg_none"
        const val BOTTOM_PADDING_KEY = "subtitle_bottom_padding"
        const val SHADOW_KEY = "subtitle_shadow"
        const val CUSTOM_FONT_PATH_KEY = "subtitle_custom_font_path"
    }
}
