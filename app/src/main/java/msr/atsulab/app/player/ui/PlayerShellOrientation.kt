package msr.atsulab.app.player.ui

import android.content.res.Configuration

internal enum class PlayerShellOrientation {
    PORTRAIT,
    LANDSCAPE;

    companion object {
        fun fromConfiguration(configuration: Configuration): PlayerShellOrientation {
            return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LANDSCAPE
            } else {
                PORTRAIT
            }
        }
    }
}
