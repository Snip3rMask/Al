package msr.atsulab.app.ui.common

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.helper.enums.Source
import msr.atsulab.app.ui.base.BaseViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class BottomSheetMediaQuickDetailViewModel(private val userRepository: UserRepository) : BaseViewModel<Unit>() {

    private val _appSetting = PublishSubject.create<AppSetting>()
    val appSetting: Observable<AppSetting>
        get() = _appSetting

    override fun loadData(param: Unit) {
        loadOnce {
            disposables.add(
                userRepository.getAppSetting()
                    .subscribe {
                        _appSetting.onNext(it)
                    }
            )
        }
    }
}