package msr.atsulab.app.ui.reorder

interface ItemMoveListener {
    fun onRowMoved(fromPosition: Int, toPosition: Int)
}