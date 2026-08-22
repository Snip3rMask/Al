package msr.atsulab.app.ui.reorder

import msr.atsulab.app.ui.base.BaseRecyclerViewAdapter

interface DragListener {
    fun onStartDrag(viewHolder: BaseRecyclerViewAdapter<*, *>.ViewHolder)
}