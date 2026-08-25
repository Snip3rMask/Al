package msr.atsulab.app.player.runtime

import msr.atsulab.app.player.domain.model.PlaybackEpisode

internal class PlaybackEpisodeNavigator {

    private var episodes: List<PlaybackEpisode> = emptyList()

    var selectedIndex: Int = INVALID_INDEX
        private set

    val currentEpisode: PlaybackEpisode?
        get() = episodes.getOrNull(selectedIndex)

    fun episodes(): List<PlaybackEpisode> = episodes.toList()

    fun reset(episodes: List<PlaybackEpisode>, requestedNumber: Int): PlaybackEpisode? {
        this.episodes = episodes.toList()
        val selectedEpisode = episodes.selectPlaybackEpisode(requestedNumber)
        selectedIndex = selectedEpisode?.let { episode ->
            episodes.indexOfFirst { candidate -> candidate === episode }
        } ?: INVALID_INDEX
        return currentEpisode
    }

    fun canMove(offset: Int): Boolean {
        if (selectedIndex == INVALID_INDEX || offset == 0) return false
        return selectedIndex + offset in episodes.indices
    }

    fun move(offset: Int): PlaybackEpisode? {
        if (selectedIndex == INVALID_INDEX || offset == 0) return null

        val targetIndex = selectedIndex + offset
        if (targetIndex !in episodes.indices) return null

        selectedIndex = targetIndex
        return currentEpisode
    }

    fun select(target: PlaybackEpisode): PlaybackEpisode? {
        val targetIndex = episodes.indexOfFirst { episode ->
            episode.url == target.url && episode.number == target.number
        }
        if (targetIndex == INVALID_INDEX) return null

        selectedIndex = targetIndex
        return currentEpisode
    }

    private companion object {
        const val INVALID_INDEX = -1
    }
}
