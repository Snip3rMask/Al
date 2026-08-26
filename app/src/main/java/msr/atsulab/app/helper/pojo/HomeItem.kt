package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.Review
import msr.atsulab.app.player.domain.model.PlaybackProgress

data class HomeItem(
    val media: List<Media> = listOf(),
    val releasingToday: List<ReleasingTodayItem> = listOf(),
    val review: Review = Review(),
    val continueWatching: List<PlaybackProgress> = listOf(),
    val viewType: Int = 0
) {
    companion object {
        const val VIEW_TYPE_HEADER = 100
        const val VIEW_TYPE_MENU = 101
        const val VIEW_TYPE_CONTINUE_WATCHING = 102
        const val VIEW_TYPE_RELEASING_TODAY = 200
        const val VIEW_TYPE_TRENDING_ANIME = 300
        const val VIEW_TYPE_TRENDING_MANGA = 301
        const val VIEW_TYPE_NEW_ANIME = 400
        const val VIEW_TYPE_NEW_MANGA = 401
        const val VIEW_TYPE_FIRST_REVIEW =  500
        const val VIEW_TYPE_REVIEW = 501
        const val VIEW_TYPE_SOCIAL = 600
    }
}