package msr.atsulab.app.ui.seasonal

import android.content.Context
import android.graphics.Color
import androidx.viewbinding.ViewBinding
import msr.atsulab.app.R
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.databinding.ListTitleBinding
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.helper.extensions.getColor
import msr.atsulab.app.helper.extensions.getString
import msr.atsulab.app.helper.extensions.getThemeNegativeColor
import msr.atsulab.app.helper.extensions.getThemeSecondaryColor
import msr.atsulab.app.helper.pojo.SeasonalItem
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

abstract class BaseSeasonalRvAdapter(
    protected val context: Context,
    list: List<SeasonalItem>,
    protected val appSetting: AppSetting,
    protected val listener: SeasonalListener
) : BaseRecyclerViewAdapter<SeasonalItem, ViewBinding>(list) {

    override fun getItemViewType(position: Int): Int {
        return list[position].viewType
    }

    protected inner class TitleViewHolder(private val binding: ListTitleBinding) : ViewHolder(binding) {
        override fun bind(item: SeasonalItem, index: Int) {
            binding.titleText.text = item.title
        }
    }

    abstract inner class ItemViewHolder(private val binding: ViewBinding) : ViewHolder(binding) {
        protected fun getCoverImage(media: Media): String {
            return media.getCoverImage(appSetting)
        }

        protected fun getTitle(media: Media): String {
            return media.getTitle(appSetting)
        }

        protected fun getMediaListStatusColor(media: Media): Int {
            return if (media.mediaListEntry == null) {
                context.getThemeNegativeColor()
            } else {
                val statusColor = media.mediaListEntry.status?.getColor()?.let {
                    Color.parseColor(it)
                } ?: context.getThemeSecondaryColor()
                statusColor
            }
        }

        protected fun getMediaListStatusIcon(media: Media): Int {
            return if (media.mediaListEntry == null) {
                R.drawable.ic_add_property
            } else {
                R.drawable.ic_filled_circle
            }
        }

        protected fun getMediaListStatusText(media: Media): String {
            return if (media.mediaListEntry == null) {
                context.getString(R.string.add_to_planning)
            } else {
                media.mediaListEntry.status?.getString(MediaType.ANIME)?.uppercase() ?: ""
            }
        }
    }

    interface SeasonalListener {
        fun navigateToMedia(media: Media)
        fun showQuickDetail(media: Media)
        fun addToPlanning(media: Media)
        fun watchTrailer(media: Media)
    }
}