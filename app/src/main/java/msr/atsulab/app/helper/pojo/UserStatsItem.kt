package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.response.anilist.UserStatisticsDetail
import msr.atsulab.app.helper.enums.MediaType

data class UserStatsItem(
    val chart: List<Chart>? = null,
    val stats: UserStatisticsDetail? = null,
    val color: String? = null,
    val label: String = "",
    var countPercentage: String = "",
    var durationPercentage: String = "",
    val mediaType: MediaType = MediaType.ANIME,
    val showRank: Boolean = false,
    val isClickable: Boolean = false,
    val viewType: Int = 0
) {
    companion object {
        const val VIEW_TYPE_PIE_CHART = 100
        const val VIEW_TYPE_LINE_CHART = 101
        const val VIEW_TYPE_BAR_CHART = 102
        const val VIEW_TYPE_INFO = 200
    }
}