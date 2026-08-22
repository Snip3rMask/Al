package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.response.anilist.Media

data class SeasonalItem(
    val media: Media = Media(),
    val title: String = "",
    val viewType: Int = 0
) {
    companion object {
        const val VIEW_TYPE_TITLE = 100
        const val VIEW_TYPE_MEDIA = 200
    }
}