package msr.atsulab.app.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.data.response.AnimeTheme
import msr.atsulab.app.data.response.AnimeThemeEntry
import msr.atsulab.app.databinding.ListMediaThemeBinding
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter
import msr.atsulab.app.ui.common.TextRvAdapter

class MediaThemesRvAdapter(
    private val context: Context,
    list: List<AnimeTheme>,
    private val listener: MediaThemesListener
) : BaseRecyclerViewAdapter<AnimeTheme, ListMediaThemeBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListMediaThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(view)
    }

    inner class ItemViewHolder(private val binding: ListMediaThemeBinding) : ViewHolder(binding) {
        override fun bind(item: AnimeTheme, index: Int) {
            with(binding) {
                themeTitle.text = item.getDisplayTitle()
                themeTitle.clicks {
                    listener.openThemeDialog(item, item.themeEntries.firstOrNull())
                }
                themeVersionRecyclerView.show(item.themeEntries.size > 1)
                themeVersionRecyclerView.adapter = TextRvAdapter(
                    context,
                    item.themeEntries.map { it.getDisplayTitle() },
                    object : TextRvAdapter.TextListener {
                        override fun getText(text: String) {
                            var selectedIndex = item.themeEntries.indexOfFirst { it.getDisplayTitle() == text }
                            if (selectedIndex == -1)
                                selectedIndex = 0
                            listener.openThemeDialog(item, item.themeEntries[selectedIndex])
                        }
                    }
                )
            }
        }
    }

    interface MediaThemesListener {
        fun openThemeDialog(animeTheme: AnimeTheme, animeThemeEntry: AnimeThemeEntry?)
    }
}