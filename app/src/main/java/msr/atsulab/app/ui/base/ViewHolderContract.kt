package msr.atsulab.app.ui.base

interface ViewHolderContract<T> {
    fun bind(item: T, index: Int)
}