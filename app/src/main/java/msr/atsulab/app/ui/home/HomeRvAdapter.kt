package msr.atsulab.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import msr.atsulab.app.R
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.User
import msr.atsulab.app.databinding.LayoutHomeContinueWatchingBinding
import msr.atsulab.app.databinding.LayoutHomeHeaderBinding
import msr.atsulab.app.databinding.LayoutHomeMenuBinding
import msr.atsulab.app.databinding.LayoutHomeReleasingTodayBinding
import msr.atsulab.app.databinding.LayoutHomeSocialBinding
import msr.atsulab.app.databinding.LayoutHomeTrendingBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.pojo.HomeItem
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class HomeRvAdapter(
    private val context: Context,
    list: List<HomeItem>,
    private val user: User?,
    private val appSetting: AppSetting,
    private val width: Int,
    private val listener: HomeListener
) : BaseRecyclerViewAdapter<HomeItem, ViewBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            HomeItem.VIEW_TYPE_HEADER -> {
                val view = LayoutHomeHeaderBinding.inflate(inflater, parent, false)
                return HeaderViewHolder(view)
            }
            HomeItem.VIEW_TYPE_MENU -> {
                val view = LayoutHomeMenuBinding.inflate(inflater, parent, false)
                return MenuViewHolder(view)
            }
            HomeItem.VIEW_TYPE_CONTINUE_WATCHING -> {
                val view = LayoutHomeContinueWatchingBinding.inflate(inflater, parent, false)
                return ContinueWatchingViewHolder(view)
            }
            HomeItem.VIEW_TYPE_RELEASING_TODAY -> {
                val view = LayoutHomeReleasingTodayBinding.inflate(inflater, parent, false)
                return ReleasingTodayViewHolder(view)
            }
            HomeItem.VIEW_TYPE_SOCIAL -> {
                val view = LayoutHomeSocialBinding.inflate(inflater, parent, false)
                return SocialViewHolder(view)
            }
            HomeItem.VIEW_TYPE_TRENDING_ANIME, HomeItem.VIEW_TYPE_TRENDING_MANGA -> {
                val view = LayoutHomeTrendingBinding.inflate(inflater, parent, false)
                return TrendingMediaViewHolder(view)
            }
            else -> {
                val view = LayoutHomeHeaderBinding.inflate(inflater, parent, false)
                return HeaderViewHolder(view)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].viewType
    }

    inner class HeaderViewHolder(private val binding: LayoutHomeHeaderBinding) : ViewHolder(binding) {
        override fun bind(item: HomeItem, index: Int) {
            binding.apply {
                if (user?.bannerImage?.isNotBlank() == true)
                    ImageUtil.loadImage(context, user.bannerImage, headerImage)

                if (user?.name?.isNotBlank() == true)
                    welcomeText.text = context.getString(R.string.hello_user, user.name)
                else
                    welcomeText.text = context.getString(R.string.hello)

                user?.let {
                    homeHeaderAvatar.show(true)
                    if (appSetting.useCircularAvatarForProfile)
                        ImageUtil.loadCircleImage(context, it.avatar.getImageUrl(appSetting), homeHeaderAvatar)
                    else
                        ImageUtil.loadRectangleImage(context, it.avatar.getImageUrl(appSetting), homeHeaderAvatar)
                } ?: homeHeaderAvatar.show(false)

                searchLayout.clicks { listener.headerListener.showSearchDialog() }
            }
        }
    }

    inner class MenuViewHolder(private val binding: LayoutHomeMenuBinding) : ViewHolder(binding) {
        override fun bind(item: HomeItem, index: Int) {
            binding.apply {
                seasonalMenu.clicks { listener.menuListener.navigateToSeasonal() }
                exploreMenu.clicks { listener.menuListener.showExploreDialog() }
                reviewsMenu.clicks { listener.menuListener.navigateToReview() }
                calendarMenu.clicks { listener.menuListener.navigateToCalendar() }
            }
        }
    }

    inner class ContinueWatchingViewHolder(private val binding: LayoutHomeContinueWatchingBinding) : ViewHolder(binding) {
        override fun bind(item: HomeItem, index: Int) {
            with(binding) {
                root.show(item.continueWatching.isNotEmpty())
                if (item.continueWatching.isNotEmpty()) {
                    continueWatchingRecyclerView.adapter = ContinueWatchingRvAdapter(
                        context,
                        item.continueWatching,
                        listener.continueWatchingListener
                    )
                }
            }
        }
    }

    inner class ReleasingTodayViewHolder(private val binding: LayoutHomeReleasingTodayBinding) : ViewHolder(binding) {
        override fun bind(item: HomeItem, index: Int) {
            with(binding) {
                if (item.releasingToday.isNotEmpty()) {
                    releasingTodayRecyclerView.adapter = ReleasingTodayRvAdapter(context, item.releasingToday, appSetting, listener.releasingTodayListener)
                    releasingTodayRecyclerView.show(true)
                    releasingTodayEmptyText.show(false)
                } else {
                    releasingTodayRecyclerView.show(false)
                    releasingTodayEmptyText.show(true)
                }
            }
        }
    }

    inner class SocialViewHolder(private val binding: LayoutHomeSocialBinding) : ViewHolder(binding) {
        override fun bind(item: HomeItem, index: Int) {
            binding.apply {
                homeSocialJoinButton.clicks { listener.socialListener.navigateToSocial() }
                root.clicks { listener.socialListener.navigateToSocial() }
            }
        }
    }

    inner class TrendingMediaViewHolder(private val binding: LayoutHomeTrendingBinding) : ViewHolder(binding) {
        override fun bind(item: HomeItem, index: Int) {
            binding.apply {
                trendingRightNowText.text = when (item.viewType) {
                    HomeItem.VIEW_TYPE_TRENDING_ANIME -> context.getString(R.string.trending_anime_right_now)
                    HomeItem.VIEW_TYPE_TRENDING_MANGA -> context.getString(R.string.trending_manga_right_now)
                    else -> ""
                }

                if (item.media.isNotEmpty()) {
                    trendingListRecyclerView.adapter = TrendingMediaRvAdapter(context, item.media, appSetting, width, listener.trendingMediaListener)
                    trendingProgressBar.show(false)
                } else {
                    trendingProgressBar.show(true)
                }
            }
        }
    }
}