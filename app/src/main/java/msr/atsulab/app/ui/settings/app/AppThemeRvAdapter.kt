package msr.atsulab.app.ui.settings.app

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import msr.atsulab.app.databinding.LayoutHeaderBinding
import msr.atsulab.app.databinding.ListAppThemeBinding
import msr.atsulab.app.helper.enums.AppTheme
import msr.atsulab.app.helper.enums.getColorName
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.helper.pojo.AppThemeItem
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

class AppThemeRvAdapter(
    private val context: Context,
    list: List<AppThemeItem>,
    private val listener: AppThemeListener?
) : BaseRecyclerViewAdapter<AppThemeItem, ViewBinding>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = LayoutHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ListAppThemeBinding.inflate(inflater, parent, false)
                AppThemeViewHolder(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].header != null) VIEW_TYPE_HEADER else VIEW_TYPE_APP_THEME
    }

    inner class HeaderViewHolder(private val binding: LayoutHeaderBinding) : ViewHolder(binding) {
        override fun bind(item: AppThemeItem, index: Int) {
            binding.headerText.text = item.header
            binding.upperHeaderDivider.root.show(true)
        }
    }

    inner class AppThemeViewHolder(private val binding: ListAppThemeBinding) : ViewHolder(binding) {
        override fun bind(item: AppThemeItem, index: Int) {
            val appTheme = item.appTheme ?: AppTheme.DEFAULT_THEME_YELLOW
            binding.apply {
                appThemeText.text = appTheme.getColorName()

                appThemePrimaryColor.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, appTheme.colors.first))
                appThemeSecondaryColor.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, appTheme.colors.second))
                appThemeNegativeColor.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, appTheme.colors.third))

                appThemeLayout.clicks {
                    listener?.getSelectedAppTheme(appTheme)
                }
            }
        }
    }

    interface AppThemeListener {
        fun getSelectedAppTheme(appTheme: AppTheme)
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 100
        private const val VIEW_TYPE_APP_THEME = 200
    }
}