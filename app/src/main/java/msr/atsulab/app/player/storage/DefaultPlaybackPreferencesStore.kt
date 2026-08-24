package msr.atsulab.app.player.storage

import android.content.Context
import androidx.core.content.edit
import msr.atsulab.app.player.domain.PlaybackSpeedOptions

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

    private companion object {
        const val PREFERENCES_NAME = "atsu_playback_preferences"
        const val SPEED_KEY = "playback_speed"
    }
}
