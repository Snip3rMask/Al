package msr.atsulab.app.ui.medialist

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.repository.BrowseRepository
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.data.response.anilist.MediaListOptions
import msr.atsulab.app.helper.enums.Source
import msr.atsulab.app.helper.extensions.applyScheduler
import msr.atsulab.app.ui.base.BaseViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observable.zip
import io.reactivex.rxjava3.subjects.PublishSubject

class BottomSheetMediaListQuickDetailViewModel(
    private val userRepository: UserRepository,
    private val browseRepository: BrowseRepository
) : BaseViewModel<BottomSheetMediaListQuickDetailParam>() {

    private val _settings = PublishSubject.create<Pair<MediaListOptions, AppSetting>>()
    val settings: Observable<Pair<MediaListOptions, AppSetting>>
        get() = _settings

    override fun loadData(param: BottomSheetMediaListQuickDetailParam) {
        loadOnce {
            val isViewer = param.userId == 0

            disposables.add(
                zip(
                    if (isViewer) userRepository.getViewer(Source.CACHE) else browseRepository.getUser(param.userId),
                    userRepository.getAppSetting()
                ) { user, appSetting ->
                    user.mediaListOptions to appSetting
                }
                    .applyScheduler()
                    .subscribe {
                        _settings.onNext(it)
                    }
            )
        }
    }
}