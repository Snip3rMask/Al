package msr.atsulab.app.ui.base

interface ViewModelContract<T> {
    fun loadData(param: T)
}