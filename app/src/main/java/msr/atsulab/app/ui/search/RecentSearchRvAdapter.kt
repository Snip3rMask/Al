package msr.atsulab.app.ui.search

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.R
import msr.atsulab.app.databinding.ListRecentSearchBinding
import msr.atsulab.app.helper.enums.SearchCategory
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter
import msr.atsulab.app.ui.search.storage.RecentSearch

class RecentSearchRvAdapter(
    private val context: Context,
    list: List<RecentSearch>,
    private val listener: Listener
) : BaseRecyclerViewAdapter<RecentSearch, ListRecentSearchBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListRecentSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListRecentSearchBinding) : ViewHolder(binding) {
        override fun bind(item: RecentSearch, index: Int) {
            with(binding) {
                recentSearchText.text = item.query
                recentSearchTypeText.text = context.getString(item.category.labelResId)
                root.clicks { listener.onRecentSearchClicked(item) }
                recentSearchRemoveButton.clicks { listener.onRecentSearchRemoved(item) }
            }
        }
    }

    interface Listener {
        fun onRecentSearchClicked(search: RecentSearch)

        fun onRecentSearchRemoved(search: RecentSearch)
    }
}

private val SearchCategory.labelResId: Int
    get() = when (this) {
        SearchCategory.ANIME -> R.string.anime
        SearchCategory.MANGA -> R.string.manga
        SearchCategory.CHARACTER -> R.string.characters
        SearchCategory.STAFF -> R.string.staff
        SearchCategory.STUDIO -> R.string.studios
        SearchCategory.USER -> R.string.users
    }
