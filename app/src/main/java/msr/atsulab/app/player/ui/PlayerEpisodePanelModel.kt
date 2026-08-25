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
        if (episodes.isEmpty()) return 0
        val number = episodeNumber(currentEpisode).coerceAtLeast(0)
        return (number / RANGE_SIZE) * RANGE_SIZE
    }

    fun ranges(
        episodes: List<PlaybackEpisode>,
        selectedRangeStart: Int
    ): List<PlayerEpisodeRangeOption> {
        if (episodes.isEmpty()) return emptyList()
        val maximum = maxEpisodeNumber(episodes)
        val selected = max(0, selectedRangeStart)
        return generateSequence(0) { start ->
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
        val normalizedStart = max(0, selectedRangeStart)
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

    private fun maxEpisodeNumber(episodes: List<PlaybackEpisode>): Int =
        episodes.maxOfOrNull(::episodeNumber) ?: 0
}
