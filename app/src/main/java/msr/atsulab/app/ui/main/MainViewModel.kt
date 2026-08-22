package msr.atsulab.app.ui.main

import msr.atsulab.app.data.repository.ContentRepository
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.helper.extensions.applyScheduler
import msr.atsulab.app.helper.service.pushnotification.PushNotificationService
import msr.atsulab.app.ui.base.BaseViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

class MainViewModel(
    private val userRepository: UserRepository,
    private val contentRepository: ContentRepository,
    private val pushNotificationService: PushNotificationService
) : BaseViewModel<Unit>() {

    val isViewerAuthenticated: Boolean
        get() = userRepository.getIsAuthenticated().blockingFirst()

    private val _unreadNotificationCount = BehaviorSubject.createDefault(0)
    val unreadNotificationCount: Observable<Int>
        get() = _unreadNotificationCount

    override fun loadData(param: Unit) {
        loadOnce {
            disposables.add(
                contentRepository.getGenres().subscribe({}, {})
            )

            disposables.add(
                contentRepository.getTags().subscribe({}, {})
            )

            disposables.add(
                userRepository.unreadNotificationCount
                    .applyScheduler()
                    .subscribe {
                        _unreadNotificationCount.onNext(it)
                    }
            )

            disposables.add(
                userRepository.getAppSetting()
                    .applyScheduler()
                    .subscribe {
                        pushNotificationService.startPushNotification()
                    }
            )
        }
    }

    fun clearUnreadNotificationCountBadge() {
        userRepository.clearUnreadNotificationCount()
    }
}