package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.response.anilist.MediaList

data class ReleasingTodayItem(
    val mediaList: MediaList,
    val episode: Int,
    val timeUntilAiring: Int
)