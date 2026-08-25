package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.SubtitleTrack
import msr.atsulab.app.player.engine.EXTERNAL_SUBTITLE_TRACK_ID

internal data class SubtitleTrackOption(
    val id: String?,
    val label: String,
    val isSelected: Boolean
)

internal object PlayerSubtitleMenuModel {

    fun options(
        tracks: List<SubtitleTrack>,
        hasExternalSubtitle: Boolean
    ): List<SubtitleTrackOption> {
        val subtitleOptions = tracks.map { track ->
            SubtitleTrackOption(
                id = track.id,
                label = track.label,
                isSelected = track.isSelected
            )
        }
        if (subtitleOptions.isEmpty() && hasExternalSubtitle) {
            subtitleOptions += SubtitleTrackOption(
                id = EXTERNAL_SUBTITLE_TRACK_ID,
                label = "English",
                isSelected = false
            )
        }

        return subtitleOptions + SubtitleTrackOption(
            id = null,
            label = "Off",
            isSelected = subtitleOptions.none(SubtitleTrackOption::isSelected)
        )
    }

}
