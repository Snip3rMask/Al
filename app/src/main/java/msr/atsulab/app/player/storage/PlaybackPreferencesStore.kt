package msr.atsulab.app.player.storage

interface PlaybackPreferencesStore {
    fun getSpeed(): Float

    fun setSpeed(speed: Float)
}
