package msr.atsulab.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.R
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.MediaList
import msr.atsulab.app.databinding.ListMediaListGridBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.pojo.ReleasingTodayItem
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.helper.utils.TimeUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter
import msr.atsulab.app.type.MediaType
import kotlin.math.abs

class ReleasingTodayRvAdapter(
    private val context: Context,
    list: List<ReleasingTodayItem>,
    private val appSetting: AppSetting,
    private val listener: HomeListener.ReleasingTodayListener
) : BaseRecyclerViewAdapter<ReleasingTodayItem, ListMediaListGridBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMediaListGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListMediaListGridBinding) : ViewHolder(binding) {
        override fun bind(item: ReleasingTodayItem, index: Int) {
            with(binding) {
                val mediaList = item.mediaList
                val media = mediaList.media

                ImageUtil.loadImage(context, media.getCoverImage(appSetting), mediaListCoverImage)

                mediaListTitleText.text = media.getTitle(appSetting)

                mediaListFormatText.text = if (item.timeUntilAiring >= 0) {
                    if (item.timeUntilAiring >= 3600) {
                        context.getString(R.string.ep_x_in_y_hours, item.episode, item.timeUntilAiring / 3600)
                    } else {
                        context.getString(R.string.ep_x_in_y_minutes, item.episode, item.timeUntilAiring / 60)
                    }
                } else {
                    if (item.timeUntilAiring <= -3600) {
                        context.getString(R.string.ep_x_y_hours_ago, item.episode, abs(item.timeUntilAiring) / 3600)
                    } else {
                        context.getString(R.string.ep_x_y_minutes_ago, item.episode, abs(item.timeUntilAiring) / 60)
                    }
                }
                mediaListProgressText.text = "${mediaList.progress} / ${mediaList.media.episodes ?: "?"}"

                mediaListAiringRootLayout.show(false)
                mediaListScoreLayout.show(false)
                mediaListProgressVolumeLayout.show(false)

                root.clicks {
                    listener.navigateToListEditor(mediaList)
                }

                mediaListTitleLayout.clicks {
                    listener.navigateToMedia(media)
                }

                mediaListProgressLayout.clicks {
                    listener.showProgressDialog(mediaList)
                }
            }
        }
    }
}