package msr.atsulab.app.ui.reorder

import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

abstract class BaseReorderRecyclerViewAdapter<T, VB: ViewBinding>(
    list: List<T>
) : BaseRecyclerViewAdapter<T, VB>(list), ItemMoveListener