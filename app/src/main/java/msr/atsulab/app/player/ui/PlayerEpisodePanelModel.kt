package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.PlaybackEpisode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class PlayerEpisodeRangeOption(
    val start: Int,
    val endInclusive: Int,
    val isSelected: Boolean
)

internal data class PlayerEpisodeGridOption(
    val episode: PlaybackEpisode,
    val number: Int,
    val isSelected: Boolean
)

internal object PlayerEpisodePanelModel {
    const val RANGE_SIZE = 100

    fun episodeNumber(episode: PlaybackEpisode?): Int {
        return episode?.number?.roundToInt()?.coerceAtLeast(0) ?: 0
    }

    fun rangeStart(episodes: List<PlaybackEpisode>, currentEpisode: PlaybackEpisode?): Int {
        if (episodes.isEmpty()) return 1
        val minimum = minEpisodeNumber(episodes)
        val requested = episodeNumber(currentEpisode)
        val normalized = max(minimum, requested)
        return ((normalized - minimum) / RANGE_SIZE) * RANGE_SIZE + minimum
    }

    fun ranges(
        episodes: List<PlaybackEpisode>,
        selectedRangeStart: Int
    ): List<PlayerEpisodeRangeOption> {
        if (episodes.isEmpty()) return emptyList()
        val minimum = minEpisodeNumber(episodes)
        val maximum = maxEpisodeNumber(episodes)
        val selected = max(minimum, selectedRangeStart)
        return generateSequence(minimum) { start ->
            val next = start + RANGE_SIZE
            if (next <= maximum) next else null
        }.map { start ->
            PlayerEpisodeRangeOption(
                start = start,
                endInclusive = min(start + RANGE_SIZE - 1, maximum),
                isSelected = start == selected
            )
        }.toList()
    }

    fun controlLabel(ranges: List<PlayerEpisodeRangeOption>): String {
        val selected = ranges.firstOrNull { it.isSelected } ?: ranges.firstOrNull()
        return selected?.let { "EPS: ${it.start}-${it.endInclusive}" }.orEmpty()
    }

    fun gridOptions(
        episodes: List<PlaybackEpisode>,
        currentEpisode: PlaybackEpisode?,
        selectedRangeStart: Int
    ): List<PlayerEpisodeGridOption> {
        val minimum = minEpisodeNumber(episodes)
        val normalizedStart = max(minimum, selectedRangeStart)
        val endNumber = normalizedStart + RANGE_SIZE - 1
        val currentNumber = episodeNumber(currentEpisode)
        return episodes.mapNotNull { episode ->
            val number = episodeNumber(episode)
            if (number < normalizedStart || number > endNumber) {
                null
            } else {
                PlayerEpisodeGridOption(
                    episode = episode,
                    number = number,
                    isSelected = number == currentNumber
                )
            }
        }
    }

    private fun minEpisodeNumber(episodes: List<PlaybackEpisode>): Int =
        episodes.minOfOrNull(::episodeNumber) ?: 1

    private fun maxEpisodeNumber(episodes: List<PlaybackEpisode>): Int =
        episodes.maxOfOrNull(::episodeNumber) ?: 0
}
