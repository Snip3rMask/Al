package msr.atsulab.app.ui.settings.account

import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.ui.base.BaseViewModel

class AccountSettingsViewModel(private val userRepository: UserRepository) : BaseViewModel<Unit>() {

    override fun loadData(param: Unit) = Unit

    fun logout() {
        userRepository.logout()
    }
}