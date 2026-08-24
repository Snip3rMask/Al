package msr.atsulab.app.player.runtime

import msr.atsulab.app.player.domain.model.PlaybackEpisode

internal fun List<PlaybackEpisode>.selectPlaybackEpisode(requestedNumber: Int): PlaybackEpisode? {
    if (isEmpty()) return null
    return firstOrNull { episode -> episode.number.toInt() == requestedNumber }
        ?: getOrNull(requestedNumber - 1)
}
