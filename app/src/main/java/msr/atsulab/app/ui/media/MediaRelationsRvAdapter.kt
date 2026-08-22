package msr.atsulab.app.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.R
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.MediaEdge
import msr.atsulab.app.databinding.ListMediaRelationBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class MediaRelationsRvAdapter(
    private val context: Context,
    list: List<MediaEdge>,
    private val appSetting: AppSetting,
    private val width: Int,
    private val listener: MediaListener.MediaRelationsListener
) : BaseRecyclerViewAdapter<MediaEdge, ListMediaRelationBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMediaRelationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        view.root.layoutParams.width = (width.toDouble() / context.resources.getInteger(R.integer.horizontalListRelationDivider)).toInt()
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListMediaRelationBinding) : ViewHolder(binding) {
        override fun bind(item: MediaEdge, index: Int) {
            binding.apply {
                ImageUtil.loadImage(context, item.node.getCoverImage(appSetting), relationImage)
                relationTitle.text = item.node.getTitle(appSetting)
                relationRelationship.text = item.getRelationTypeString()
                relationFormat.text = item.node.getFormattedMediaFormat(true)
                relationImage.clicks {
                    listener.navigateToMedia(item.node)
                }
            }
        }
    }
}