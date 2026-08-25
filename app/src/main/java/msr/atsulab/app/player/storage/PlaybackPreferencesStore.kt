package msr.atsulab.app.player.storage

import msr.atsulab.app.player.domain.model.SubtitleStyle

interface PlaybackPreferencesStore {
    fun getSpeed(): Float

    fun setSpeed(speed: Float)

    fun getSubtitleStyle(): SubtitleStyle

    fun setSubtitleStyle(style: SubtitleStyle)
}
