package msr.atsulab.app.player.storage

import msr.atsulab.app.player.domain.model.SubtitleStyle

interface PlaybackPreferencesStore {
    fun getSpeed(): Float

    fun setSpeed(speed: Float)

    fun isFrameCaptureEnabled(): Boolean

    fun setFrameCaptureEnabled(enabled: Boolean)

    fun isFrameCaptureAlwaysVisible(): Boolean

    fun setFrameCaptureAlwaysVisible(enabled: Boolean)

    fun getFrameCapturePositionX(): Float

    fun getFrameCapturePositionY(): Float

    fun setFrameCapturePosition(xFraction: Float, yFraction: Float)

    fun getFrameCaptureDirectoryUri(): String

    fun setFrameCaptureDirectoryUri(uri: String)

    fun clearFrameCapturePosition()

    fun getSubtitleStyle(): SubtitleStyle

    fun setSubtitleStyle(style: SubtitleStyle)

    fun getDownloadParallelSegments(): Int

    fun setDownloadParallelSegments(count: Int)

    fun getDownloadStorageLocation(): DownloadStorageLocation

    fun setDownloadStorageLocation(location: DownloadStorageLocation)
}
