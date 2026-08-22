package msr.atsulab.app.ui.staff.character

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.databinding.ListStaffCharacterMediaBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter
import msr.atsulab.app.ui.staff.StaffListener

class StaffCharacterListMediaRvAdapter(
    private val context: Context,
    list: List<Media>,
    private val appSetting: AppSetting,
    private val listener: StaffCharacterListListener
) : BaseRecyclerViewAdapter<Media, ListStaffCharacterMediaBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListStaffCharacterMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListStaffCharacterMediaBinding) : ViewHolder(binding) {
        override fun bind(item: Media, index: Int) {
            binding.apply {
                mediaName.text = item.getTitle(appSetting)
                ImageUtil.loadRectangleImage(context, item.getCoverImage(appSetting), mediaCoverImage)
                root.clicks { listener.navigateToMedia(item) }
            }
        }
    }
}