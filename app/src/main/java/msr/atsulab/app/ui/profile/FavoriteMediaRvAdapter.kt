package msr.atsulab.app.ui.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.databinding.ListRectangleBinding
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class FavoriteMediaRvAdapter(
    private val context: Context,
    list: List<Media>,
    private val mediaType: MediaType,
    private val appSetting: AppSetting,
    private val listener: ProfileListener.FavoriteMediaListener
) : BaseRecyclerViewAdapter<Media, ListRectangleBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListRectangleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListRectangleBinding) : ViewHolder(binding) {
        override fun bind(item: Media, index: Int) {
            binding.apply {
                val image = item.getCoverImage(appSetting)
                ImageUtil.loadImage(context, image, rectangleItemImage)
                rectangleItemText.show(false)
                root.clicks { listener.navigateToMedia(item, mediaType) }
            }
        }
    }
}