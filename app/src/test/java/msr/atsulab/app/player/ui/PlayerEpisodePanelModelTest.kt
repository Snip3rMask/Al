package msr.atsulab.app.player.ui

import msr.atsulab.app.player.domain.model.PlaybackEpisode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerEpisodePanelModelTest {

    @Test
    fun `ranges follow minimum episode and fixed size`() {
        val episodes = (50..260).map { episode(it.toFloat()) }

        val ranges = PlayerEpisodePanelModel.ranges(episodes, selectedRangeStart = 150)

        assertEquals(listOf(50, 150, 250), ranges.map { it.start })
        assertEquals(149, ranges[0].endInclusive)
        assertEquals(249, ranges[1].endInclusive)
        assertEquals(260, ranges[2].endInclusive)
    }

    @Test
    fun `range start snaps current episode into its range`() {
        val episodes = (101..180).map { episode(it.toFloat()) }
        val current = episode(151f)

        assertEquals(150, PlayerEpisodePanelModel.rangeStart(episodes, current))
    }

    @Test
    fun `grid options only contain selected range and mark active episode`() {
        val episodes = listOf(episode(99f), episode(100f), episode(150f), episode(201f))

        val options = PlayerEpisodePanelModel.gridOptions(
            episodes = episodes,
            currentEpisode = episode(150f),
            selectedRangeStart = 100
        )

        assertEquals(listOf(100, 150), options.map { it.number })
        assertEquals(listOf(false, true), options.map { it.isSelected })
    }

    @Test
    fun `range label uses selected bounds`() {
        val episodes = (90..210).map { episode(it.toFloat()) }
        val ranges = PlayerEpisodePanelModel.ranges(episodes, selectedRangeStart = 190)

        assertEquals("EPS: 190-210", PlayerEpisodePanelModel.controlLabel(ranges))
    }

    private fun episode(number: Float) = PlaybackEpisode(
        name = "Episode ${number.toInt()}",
        url = "episode-$number",
        number = number
    )
}
