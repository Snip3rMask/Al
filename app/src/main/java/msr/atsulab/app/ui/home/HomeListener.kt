package msr.atsulab.app.ui.home

import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.MediaList
import msr.atsulab.app.player.domain.model.PlaybackProgress

interface HomeListener {

    interface HeaderListener {
        fun showSearchDialog()
    }

    interface ContinueWatchingListener {
        fun resumePlayback(progress: PlaybackProgress)
        fun removeProgress(progress: PlaybackProgress)
    }

    interface MenuListener {
        fun navigateToSeasonal()
        fun showExploreDialog()
        fun navigateToReview()
        fun navigateToCalendar()
    }

    interface ReleasingTodayListener {
        fun navigateToMedia(media: Media)
        fun navigateToListEditor(mediaList: MediaList)
        fun showProgressDialog(mediaList: MediaList)
    }

    interface SocialListener {
        fun navigateToSocial()
    }

    interface TrendingMediaListener {
        fun navigateToMedia(media: Media)
    }

    interface NewMediaListener {

    }

    interface RecentReviewsListener {

    }

    val headerListener: HeaderListener
    val menuListener: MenuListener
    val continueWatchingListener: ContinueWatchingListener
    val releasingTodayListener: ReleasingTodayListener
    val socialListener: SocialListener
    val trendingMediaListener: TrendingMediaListener
    val newMediaListener: NewMediaListener
    val recentReviewsListener: RecentReviewsListener
}