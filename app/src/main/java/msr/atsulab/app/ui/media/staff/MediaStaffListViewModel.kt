package msr.atsulab.app.ui.media.staff

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.repository.BrowseRepository
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.data.response.anilist.StaffEdge
import msr.atsulab.app.helper.extensions.applyScheduler
import msr.atsulab.app.helper.extensions.getStringResource
import msr.atsulab.app.ui.base.BaseViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

class MediaStaffListViewModel(
    private val userRepository: UserRepository,
    private val browseRepository: BrowseRepository
) : BaseViewModel<MediaStaffListParam>() {

    private val _appSetting = PublishSubject.create<AppSetting>()
    val appSetting: Observable<AppSetting>
        get() = _appSetting

    private val _staff = BehaviorSubject.createDefault<List<StaffEdge>>(listOf())
    val staff: Observable<List<StaffEdge>>
        get() = _staff

    private val _emptyLayoutVisibility = BehaviorSubject.createDefault(false)
    val emptyLayoutVisibility: Observable<Boolean>
        get() = _emptyLayoutVisibility

    private var mediaId = 0

    private var hasNextPage = false
    private var currentPage = 0

    override fun loadData(param: MediaStaffListParam) {
        loadOnce {
            mediaId = param.mediaId

            disposables.add(
                userRepository.getAppSetting()
                    .applyScheduler()
                    .subscribe {
                        _appSetting.onNext(it)
                        loadStaffs()
                    }
            )
        }
    }

    fun reloadData() {
        loadStaffs()
    }

    fun loadNextPage() {
        if ((state == State.LOADED || state == State.ERROR) && hasNextPage) {
            val currentStaffs = ArrayList(_staff.value ?: listOf())
            currentStaffs.add(null)
            _staff.onNext(currentStaffs)

            loadStaffs(true)
        }
    }

    private fun loadStaffs(isLoadingNextPage: Boolean = false) {
        if (!isLoadingNextPage)
            _loading.onNext(true)

        state = State.LOADING

        disposables.add(
            browseRepository.getMediaStaff(mediaId, if (isLoadingNextPage) currentPage + 1 else 1)
                .applyScheduler()
                .doFinally {
                    if (!isLoadingNextPage) {
                        _loading.onNext(false)
                        _emptyLayoutVisibility.onNext(_staff.value.isNullOrEmpty())
                    }
                }
                .subscribe(
                    { (pageInfo, staffEdges) ->
                        hasNextPage = pageInfo.hasNextPage
                        currentPage = pageInfo.currentPage

                        if (isLoadingNextPage) {
                            val currentStaffs = ArrayList(_staff.value ?: listOf())
                            currentStaffs.remove(null)
                            currentStaffs.addAll(staffEdges)
                            _staff.onNext(currentStaffs)
                        } else {
                            _staff.onNext(staffEdges)
                        }

                        state = State.LOADED
                    },
                    {
                        if (isLoadingNextPage) {
                            val currentStaffs = ArrayList(_staff.value ?: listOf())
                            currentStaffs.remove(null)
                            _staff.onNext(currentStaffs)
                        }

                        _error.onNext(it.getStringResource())
                        state = State.ERROR
                    }
                )
        )
    }
}