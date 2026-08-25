package msr.atsulab.app.player.ui

import androidx.annotation.StringRes
import msr.atsulab.app.R
import msr.atsulab.app.player.domain.model.SkipInterval

internal object PlayerSkipController {

    fun activeInterval(
        intervals: List<SkipInterval>,
        positionMs: Long
    ): SkipInterval? {
        return intervals.firstOrNull { interval ->
            positionMs >= interval.startMs && positionMs < interval.endMs - ACTIVE_TAIL_MS
        }
    }

    @StringRes
    fun titleResource(interval: SkipInterval): Int {
        return if (interval.isEnding) {
            R.string.player_skip_outro
        } else {
            R.string.player_skip_intro
        }
    }
}
