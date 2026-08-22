package msr.atsulab.app.ui.social

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.User
import msr.atsulab.app.databinding.ListLikeBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.pojo.ListItem
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class LikeRvAdapter(
    private val context: Context,
    list: List<User>,
    private val appSetting: AppSetting,
    private val likeListener: LikeListener
) : BaseRecyclerViewAdapter<User, ListLikeBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListLikeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListLikeBinding) : ViewHolder(binding) {
        override fun bind(item: User, index: Int) {
            with(binding) {
                ImageUtil.loadCircleImage(context, item.avatar.getImageUrl(appSetting), likeAvatar)
                likeName.text = item.name
                root.clicks {
                    likeListener.navigateToUser(item)
                }
            }
        }
    }

    interface LikeListener {
        fun navigateToUser(user: User)
    }
}