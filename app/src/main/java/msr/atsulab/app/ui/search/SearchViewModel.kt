package msr.atsulab.app.ui.search

import msr.atsulab.app.R
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.repository.ContentRepository
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.data.response.anilist.*
import msr.atsulab.app.helper.enums.SearchCategory
import msr.atsulab.app.helper.extensions.applyScheduler
import msr.atsulab.app.helper.extensions.getStringResource
import msr.atsulab.app.helper.pojo.ListItem
import msr.atsulab.app.helper.pojo.SearchItem
import msr.atsulab.app.ui.base.BaseViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import msr.atsulab.app.type.MediaType

class SearchViewModel(
    private val userRepository: UserRepository,
    private val contentRepository: ContentRepository
) : BaseViewModel<SearchParam>() {

    private val _appSetting = PublishSubject.create<AppSetting>()
    val appSetting: Observable<AppSetting>
        get() = _appSetting

    private val _emptyLayoutVisibility = BehaviorSubject.createDefault(false)
    val emptyLayoutVisibility: Observable<Boolean>
        get() = _emptyLayoutVisibility

    private val _searchItems = BehaviorSubject.createDefault<List<SearchItem?>>(listOf())
    val searchItems: Observable<List<SearchItem?>>
        get() = _searchItems

    private val _searchCategoryList = PublishSubject.create<List<ListItem<SearchCategory>>>()
    val searchCategoryList: Observable<List<ListItem<SearchCategory>>>
        get() = _searchCategoryList

    private val _searchPlaceholderText = BehaviorSubject.createDefault(R.string.search_anime)
    val searchPlaceholderText: Observable<Int>
        get() = _searchPlaceholderText

    private val _scrollToTopTrigger = PublishSubject.create<Unit>()
    val scrollToTopTrigger: Observable<Unit>
        get() = _scrollToTopTrigger

    private var currentSearchCategory = SearchCategory.ANIME
    private var currentSearchQuery = ""

    private var hasNextPage = false
    private var currentPage = 0

    override fun loadData(param: SearchParam) {
        currentSearchCategory = param.searchCategory

        loadOnce {
            updateSelectedSearchCategory(currentSearchCategory)

            disposables.add(
                userRepository.getAppSetting()
                    .applyScheduler()
                    .subscribe {
                        _appSetting.onNext(it)
                    }
            )
        }
    }

    fun reloadData() {
        doSearch(currentSearchQuery)
    }

    fun loadNextPage() {
        if ((state == State.LOADED || state == State.ERROR) && hasNextPage) {
            val currentSearchItems = ArrayList(_searchItems.value ?: listOf())
            currentSearchItems.add(null)
            _searchItems.onNext(currentSearchItems)

            doSearch(currentSearchQuery, true)
        }
    }

    fun doSearch(searchQuery: String, isLoadingNextPage: Boolean = false) {
        if (searchQuery.isBlank()) {
            _loading.onNext(false)
            _emptyLayoutVisibility.onNext(false)
            currentSearchQuery = searchQuery
            hasNextPage = false
            currentPage = 0
            _searchItems.onNext(listOf())
            return
        }

        if (searchQuery.isNotBlank() && !isLoadingNextPage)
            _loading.onNext(true)

        state = State.LOADING

        currentSearchQuery = searchQuery

        val page = if (isLoadingNextPage) currentPage + 1 else 1

        disposables.add(
            when (currentSearchCategory) {
                SearchCategory.ANIME -> contentRepository.searchMedia(searchQuery, MediaType.ANIME, null, page)
                SearchCategory.MANGA -> contentRepository.searchMedia(searchQuery, MediaType.MANGA, null, page)
                SearchCategory.CHARACTER -> contentRepository.searchCharacter(searchQuery, page)
                SearchCategory.STAFF -> contentRepository.searchStaff(searchQuery, page)
                SearchCategory.STUDIO -> contentRepository.searchStudio(searchQuery, page)
                SearchCategory.USER -> contentRepository.searchUser(searchQuery, page)
            }
                .applyScheduler()
                .doFinally {
                    if (searchQuery.isNotBlank() && !isLoadingNextPage) {
                        _loading.onNext(false)
                        _emptyLayoutVisibility.onNext(_searchItems.value?.isEmpty() == true)
                    }
                }
                .subscribe(
                    {
                        hasNextPage = it.pageInfo.hasNextPage
                        currentPage = it.pageInfo.currentPage

                        val newSearchItems = it.data.map {
                            SearchItem(
                                media = it as? Media ?: Media(),
                                character = it as? Character ?: Character(),
                                staff = it as? Staff ?: Staff(),
                                studio = it as? Studio ?: Studio(),
                                user = it as? User ?: User(),
                                searchCategory = currentSearchCategory
                            )
                        }

                        if (isLoadingNextPage) {
                            val currentSearchItems = ArrayList(_searchItems.value ?: listOf())
                            currentSearchItems.remove(null)
                            currentSearchItems.addAll(newSearchItems)
                            _searchItems.onNext(currentSearchItems)
                        } else {
                            _searchItems.onNext(newSearchItems)
                            _scrollToTopTrigger.onNext(Unit)
                        }

                        state = State.LOADED
                    },
                    {
                        if (isLoadingNextPage) {
                            val currentSearchItems = ArrayList(_searchItems.value ?: listOf())
                            currentSearchItems.remove(null)
                            _searchItems.onNext(currentSearchItems)
                        }

                        _error.onNext(it.getStringResource())
                        state = State.ERROR
                    }
                )
        )
    }

    fun updateSelectedSearchCategory(newSearchCategory: SearchCategory) {
        currentSearchCategory = newSearchCategory
        _searchPlaceholderText.onNext(
            when (newSearchCategory) {
                SearchCategory.ANIME -> R.string.search_anime
                SearchCategory.MANGA -> R.string.search_manga
                SearchCategory.CHARACTER -> R.string.search_characters
                SearchCategory.STAFF -> R.string.search_staff
                SearchCategory.STUDIO -> R.string.search_studios
                SearchCategory.USER -> R.string.search_users
            }
        )
        reloadData()
    }

    fun loadSearchCategories() {
        val list = ArrayList<ListItem<SearchCategory>>()
        list.add(ListItem(R.string.anime, SearchCategory.ANIME))
        list.add(ListItem(R.string.manga, SearchCategory.MANGA))
        list.add(ListItem(R.string.characters, SearchCategory.CHARACTER))
        list.add(ListItem(R.string.staff, SearchCategory.STAFF))
        list.add(ListItem(R.string.studios, SearchCategory.STUDIO))
        list.add(ListItem(R.string.users, SearchCategory.USER))
        _searchCategoryList.onNext(list)
    }
}