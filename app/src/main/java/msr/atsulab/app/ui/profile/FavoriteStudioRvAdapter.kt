package msr.atsulab.app.ui.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.response.anilist.Studio
import msr.atsulab.app.databinding.ListCardTextBinding
import msr.atsulab.app.databinding.ListCircularBinding
import msr.atsulab.app.databinding.ListRectangleBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class FavoriteStudioRvAdapter(
    private val context: Context,
    list: List<Studio>,
    private val listener: ProfileListener.FavoriteStudioListener
) : BaseRecyclerViewAdapter<Studio, ListCardTextBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListCardTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListCardTextBinding) : ViewHolder(binding) {
        override fun bind(item: Studio, index: Int) {
            binding.apply {
                cardIcon.show(false)
                cardText.text = item.name
                root.clicks { listener.navigateToStudio(item) }
            }
        }
    }
}