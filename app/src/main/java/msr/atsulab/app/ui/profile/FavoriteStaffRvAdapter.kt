package msr.atsulab.app.ui.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Staff
import msr.atsulab.app.databinding.ListCircularBinding
import msr.atsulab.app.databinding.ListRectangleBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class FavoriteStaffRvAdapter(
    private val context: Context,
    list: List<Staff>,
    private val appSetting: AppSetting,
    private val listener: ProfileListener.FavoriteStaffListener
) : BaseRecyclerViewAdapter<Staff, ListRectangleBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListRectangleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListRectangleBinding) : ViewHolder(binding) {
        override fun bind(item: Staff, index: Int) {
            binding.apply {
                val image = item.getImage(appSetting)
                ImageUtil.loadImage(context, image, rectangleItemImage)
                rectangleItemText.show(false)
                root.clicks { listener.navigateToStaff(item) }
            }
        }
    }
}