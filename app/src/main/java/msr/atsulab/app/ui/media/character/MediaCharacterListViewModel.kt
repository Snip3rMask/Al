package msr.atsulab.app.ui.media.character

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.repository.BrowseRepository
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.data.response.anilist.CharacterEdge
import msr.atsulab.app.helper.extensions.applyScheduler
import msr.atsulab.app.helper.extensions.getNonUnknownValues
import msr.atsulab.app.helper.extensions.getString
import msr.atsulab.app.helper.extensions.getStringResource
import msr.atsulab.app.helper.pojo.ListItem
import msr.atsulab.app.ui.base.BaseViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import msr.atsulab.app.type.StaffLanguage

class MediaCharacterListViewModel(
    private val userRepository: UserRepository,
    private val browseRepository: BrowseRepository
) : BaseViewModel<MediaCharacterListParam>() {

    private val _appSetting = PublishSubject.create<AppSetting>()
    val appSetting: Observable<AppSetting>
        get() = _appSetting

    private val _characters = BehaviorSubject.createDefault<List<CharacterEdge>>(listOf())
    val characters: Observable<List<CharacterEdge>>
        get() = _characters

    private val _emptyLayoutVisibility = BehaviorSubject.createDefault(false)
    val emptyLayoutVisibility: Observable<Boolean>
        get() = _emptyLayoutVisibility

    private val _voiceActorLanguages = PublishSubject.create<List<ListItem<StaffLanguage>>>()
    val voiceActorLanguages: Observable<List<ListItem<StaffLanguage>>>
        get() = _voiceActorLanguages

    private var mediaId = 0
    private var selectedLanguage = StaffLanguage.JAPANESE

    private var hasNextPage = false
    private var currentPage = 0

    override fun loadData(param: MediaCharacterListParam) {
        loadOnce {
            mediaId = param.mediaId

            disposables.add(
                userRepository.getAppSetting()
                    .applyScheduler()
                    .subscribe {
                        _appSetting.onNext(it)
                        loadCharacters()
                    }
            )
        }
    }

    fun reloadData() {
        loadCharacters()
    }

    fun loadNextPage() {
        if ((state == State.LOADED || state == State.ERROR) && hasNextPage) {
            val currentCharacters = ArrayList(_characters.value ?: listOf())
            currentCharacters.add(null)
            _characters.onNext(currentCharacters)

            loadCharacters(true)
        }
    }

    private fun loadCharacters(isLoadingNextPage: Boolean = false) {
        if (!isLoadingNextPage)
            _loading.onNext(true)

        state = State.LOADING

        disposables.add(
            browseRepository.getMediaCharacters(mediaId, if (isLoadingNextPage) currentPage + 1 else 1, selectedLanguage)
                .applyScheduler()
                .doFinally {
                    if (!isLoadingNextPage) {
                        _loading.onNext(false)
                        _emptyLayoutVisibility.onNext(_characters.value.isNullOrEmpty())
                    }
                }
                .subscribe(
                    { (pageInfo, characterEdges) ->
                        hasNextPage = pageInfo.hasNextPage
                        currentPage = pageInfo.currentPage

                        if (isLoadingNextPage) {
                            val currentCharacters = ArrayList(_characters.value ?: listOf())
                            currentCharacters.remove(null)
                            currentCharacters.addAll(characterEdges)
                            _characters.onNext(currentCharacters)
                        } else {
                            _characters.onNext(characterEdges)
                        }

                        state = State.LOADED
                    },
                    {
                        if (isLoadingNextPage) {
                            val currentCharacters = ArrayList(_characters.value ?: listOf())
                            currentCharacters.remove(null)
                            _characters.onNext(currentCharacters)
                        }

                        _error.onNext(it.getStringResource())
                        state = State.ERROR
                    }
                )
        )
    }

    fun updateVoiceActorLanguage(newLanguage: StaffLanguage) {
        selectedLanguage = newLanguage
        reloadData()
    }

    fun loadVoiceActorLanguages() {
        _voiceActorLanguages.onNext(getNonUnknownValues<StaffLanguage>().map { ListItem(it.getString(), it) })
    }
}