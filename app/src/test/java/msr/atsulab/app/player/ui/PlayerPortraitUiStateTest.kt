package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.PlaybackEpisode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerPortraitUiStateTest {

    @Test
    fun `portrait state maps selected range into episode grid options`() {
        val episodes = (99..201).map { episode(it.toFloat()) }
        val current = episode(150f)
        val state = PlayerPortraitUiState(
            episodes = episodes,
            currentEpisode = current,
            rangeStart = 100
        )

        val options = PlayerEpisodePanelModel.gridOptions(
            episodes = state.episodes,
            currentEpisode = state.currentEpisode,
            selectedRangeStart = state.rangeStart
        )

        assertEquals((100..199).toList(), options.map { it.number })
        assertEquals(150, options.first { it.isSelected }.number)
    }

    @Test
    fun `portrait state keeps loading source contract separate from failure`() {
        val state = PlayerPortraitUiState()

        assertEquals(true, state.isLoadingSources)
        assertEquals(false, state.isMoreServersLoading)
        assertEquals(false, state.hasAllServersFailed)
        assertEquals(emptyList<PlaybackEpisode>(), state.episodes)
    }

    private fun episode(number: Float) = PlaybackEpisode(
        name = "Episode ${number.toInt()}",
        url = "episode-$number",
        number = number
    )
}
