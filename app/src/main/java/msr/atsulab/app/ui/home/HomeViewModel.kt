package msr.atsulab.app.ui.home

import msr.atsulab.app.R
import msr.atsulab.app.data.repository.ContentRepository
import msr.atsulab.app.data.repository.MediaListRepository
import msr.atsulab.app.data.repository.UserRepository
import msr.atsulab.app.data.response.anilist.MediaList
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.helper.enums.SearchCategory
import msr.atsulab.app.helper.enums.Source
import msr.atsulab.app.helper.extensions.applyScheduler
import msr.atsulab.app.helper.extensions.getStringResource
import msr.atsulab.app.helper.pojo.HomeAdapterComponent
import msr.atsulab.app.helper.pojo.HomeItem
import msr.atsulab.app.player.domain.model.PlaybackProgress
import msr.atsulab.app.player.domain.repository.PlaybackProgressRepository
import msr.atsulab.app.helper.pojo.ListItem
import msr.atsulab.app.helper.pojo.ReleasingTodayItem
import msr.atsulab.app.type.MediaListStatus
import msr.atsulab.app.ui.base.BaseViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlin.math.abs

internal fun applyContinueWatching(
    items: List<HomeItem>,
    entries: List<PlaybackProgress>
): List<HomeItem> {
    val index = items.indexOfFirst { it.viewType == HomeItem.VIEW_TYPE_CONTINUE_WATCHING }
    if (index == -1) return items
    return items.mapIndexed { position, item ->
        if (position == index) item.copy(continueWatching = entries) else item
    }
}

class HomeViewModel(
    private val contentRepository: ContentRepository,
    private val userRepository: UserRepository,
    private val mediaListRepository: MediaListRepository,
    private val playbackProgressRepository: PlaybackProgressRepository
) : BaseViewModel<Unit>() {

    private val _homeItemList = BehaviorSubject.createDefault(listOf<HomeItem>())
    val homeItemList: Observable<List<HomeItem>>
        get() = _homeItemList

    private val _adapterComponent = PublishSubject.create<HomeAdapterComponent>()
    val adapterComponent: Observable<HomeAdapterComponent>
        get() = _adapterComponent

    private val _searchCategoryList = PublishSubject.create<List<ListItem<SearchCategory>>>()
    val searchCategoryList: Observable<List<ListItem<SearchCategory>>>
        get() = _searchCategoryList

    private val _exploreCategoryList = PublishSubject.create<List<ListItem<SearchCategory>>>()
    val exploreCategoryList: Observable<List<ListItem<SearchCategory>>>
        get() = _exploreCategoryList

    override fun loadData(param: Unit) {
        loadOnce {
            disposables.add(
                playbackProgressRepository.observeAll()
                    .applyScheduler()
                    .subscribe(
                        { updateContinueWatching(it) },
                        { it.printStackTrace() }
                    )
            )
            disposables.add(
                userRepository.getAppSetting()
                    .zipWith(userRepository.getViewer(Source.CACHE)) { appSetting, user ->
                        HomeAdapterComponent(user, appSetting)
                    }
                    .applyScheduler()
                    .subscribe {
                        _adapterComponent.onNext(it)
                        getHomeData()
                    }
            )

            disposables.add(
                mediaListRepository.releasingTodayTrigger
                    .applyScheduler()
                    .subscribe {
                        disposables.add(
                            userRepository.getViewer(Source.CACHE)
                                .flatMap { user ->
                                    mediaListRepository.hasBigList(user, MediaType.ANIME)
                                        .map {
                                            user to it
                                        }
                                }
                                .filter {
                                    if (it.second) {
                                        val currentHomeList = ArrayList(_homeItemList.value ?: listOf())
                                        val index = currentHomeList.indexOfFirst { it.viewType == HomeItem.VIEW_TYPE_RELEASING_TODAY }
                                        if (index != -1) {
                                            currentHomeList.removeAt(index)
                                        }
                                        _homeItemList.onNext(currentHomeList)
                                    }
                                    !it.second
                                }
                                .map {
                                    it.first
                                }
                                .flatMap {
                                    mediaListRepository.getMediaListCollection(Source.CACHE, it, MediaType.ANIME)
                                }
                                .map {
                                    it.lists.filter { it.status != MediaListStatus.DROPPED }
                                }
                                .map {
                                    val releasingTodayItem = mutableSetOf<ReleasingTodayItem>()

                                    it.forEach {
                                        it.entries.forEach { mediaList ->
                                            val media = mediaList.media
                                            var currentEpisode = 0

                                            if (media.nextAiringEpisode != null) {
                                                if (media.nextAiringEpisode.timeUntilAiring < 3600 * 24) {
                                                    releasingTodayItem.add(ReleasingTodayItem(mediaList, media.nextAiringEpisode.episode, media.nextAiringEpisode.timeUntilAiring))
                                                } else {
                                                    currentEpisode = media.nextAiringEpisode.episode - 1
                                                }
                                            }

                                            if (media.airingSchedule.nodes.isNotEmpty()) {
                                                val currentEpisodeSchedule = media.airingSchedule.nodes.find { it.episode == currentEpisode }
                                                if (currentEpisodeSchedule != null && abs(currentEpisodeSchedule.timeUntilAiring) < 3600 * 24) {
                                                    releasingTodayItem.add(ReleasingTodayItem(mediaList, currentEpisodeSchedule.episode, currentEpisodeSchedule.timeUntilAiring))
                                                }
                                            }
                                        }
                                    }

                                    releasingTodayItem.sortedBy { it.timeUntilAiring }
                                }
                                .subscribe(
                                    {
                                        val currentHomeList = ArrayList(_homeItemList.value ?: listOf())
                                        val releasingTodayIndex = currentHomeList.indexOfFirst { it.viewType == HomeItem.VIEW_TYPE_RELEASING_TODAY }
                                        if (releasingTodayIndex != - 1) {
                                            currentHomeList.removeAt(releasingTodayIndex)
                                        }
                                        val socialIndex = currentHomeList.indexOfFirst { it.viewType == HomeItem.VIEW_TYPE_SOCIAL } + 1
                                        if (socialIndex != -1) {
                                            currentHomeList.add(socialIndex, HomeItem(releasingToday = it, viewType = HomeItem.VIEW_TYPE_RELEASING_TODAY))
                                        }
                                        _homeItemList.onNext(currentHomeList)
                                    },
                                    {
                                        it.printStackTrace()
                                    }
                                )
                        )
                    }
            )
        }
    }

    fun reloadData() {
        getHomeData(true)
    }

    fun removeContinueWatching(progress: PlaybackProgress) {
        disposables.add(
            playbackProgressRepository.remove(progress.aniListId, progress.playbackId, progress.episodeUrl)
                .applyScheduler()
                .subscribe({}, { it.printStackTrace() })
        )
    }

    private fun updateContinueWatching(entries: List<PlaybackProgress>) {
        _homeItemList.onNext(applyContinueWatching(_homeItemList.value ?: listOf(), entries))
    }

    private fun getHomeData(isReloading: Boolean = false) {
        if (!isReloading && state == State.LOADED) return

        if (isReloading)
            _loading.onNext(true)
        else
            _homeItemList.onNext(
                listOf(
                    HomeItem(viewType = HomeItem.VIEW_TYPE_HEADER),
                    HomeItem(viewType = HomeItem.VIEW_TYPE_MENU),
                    HomeItem(continueWatching = emptyList(), viewType = HomeItem.VIEW_TYPE_CONTINUE_WATCHING),
                    HomeItem(viewType = HomeItem.VIEW_TYPE_SOCIAL),
                    HomeItem(viewType = HomeItem.VIEW_TYPE_TRENDING_ANIME),
                    HomeItem(viewType = HomeItem.VIEW_TYPE_TRENDING_MANGA)
                )
            )

        requestHomeData(if (isReloading) Source.NETWORK else null)
    }

    private fun requestHomeData(source: Source?) {
        state = State.LOADING

        disposables.add(
            contentRepository.getHomeData(source)
                .applyScheduler()
                .subscribe(
                    {
                        val currentHomeItems = ArrayList(_homeItemList.value ?: listOf())
                        val trendingAnimeIndex = currentHomeItems.indexOfFirst { it.viewType == HomeItem.VIEW_TYPE_TRENDING_ANIME }
                        if (trendingAnimeIndex != -1) {
                            currentHomeItems[trendingAnimeIndex] = HomeItem(media = it.trendingAnime, viewType = HomeItem.VIEW_TYPE_TRENDING_ANIME)
                        }
                        val trendingMangaIndex = currentHomeItems.indexOfFirst { it.viewType == HomeItem.VIEW_TYPE_TRENDING_MANGA }
                        if (trendingMangaIndex != -1) {
                            currentHomeItems[trendingMangaIndex] = HomeItem(media = it.trendingManga, viewType = HomeItem.VIEW_TYPE_TRENDING_MANGA)
                        }
                        _homeItemList.onNext(currentHomeItems)
                        _loading.onNext(false)
                        state = State.LOADED
                    },
                    {
                        if (source == Source.CACHE) {
                            _error.onNext(it.getStringResource())
                            _loading.onNext(false)
                            state = State.ERROR
                        } else {
                            requestHomeData(Source.CACHE)
                        }
                    }
                )
        )
    }

    fun loadSearchCategories() {
        val list = ArrayList<ListItem<SearchCategory>>()
        list.add(ListItem(R.string.search_anime, SearchCategory.ANIME))
        list.add(ListItem(R.string.search_manga, SearchCategory.MANGA))
        list.add(ListItem(R.string.search_characters, SearchCategory.CHARACTER))
        list.add(ListItem(R.string.search_staff, SearchCategory.STAFF))
        list.add(ListItem(R.string.search_studios, SearchCategory.STUDIO))
        list.add(ListItem(R.string.search_users, SearchCategory.USER))
        _searchCategoryList.onNext(list)
    }

    fun loadExploreCategories() {
        val list = ArrayList<ListItem<SearchCategory>>()
        list.add(ListItem(R.string.explore_anime, SearchCategory.ANIME))
        list.add(ListItem(R.string.explore_manga, SearchCategory.MANGA))
        list.add(ListItem(R.string.explore_characters, SearchCategory.CHARACTER))
        list.add(ListItem(R.string.explore_staff, SearchCategory.STAFF))
        list.add(ListItem(R.string.explore_studios, SearchCategory.STUDIO))
        _exploreCategoryList.onNext(list)
    }

    fun updateProgress(mediaList: MediaList, newProgress: Int) {
        if (mediaList.progress == newProgress)
            return

        val maxProgress = mediaList.media.episodes

        var targetProgress = newProgress
        var status: MediaListStatus? = null
        var repeat: Int? = null

        if (maxProgress != null && newProgress >= maxProgress) {
            if (mediaList.status == MediaListStatus.REPEATING)
                repeat = mediaList.repeat + 1

            status = MediaListStatus.COMPLETED
            targetProgress = maxProgress
        } else {
            if (mediaList.status == MediaListStatus.PLANNING ||
                mediaList.status == MediaListStatus.PAUSED ||
                mediaList.status == MediaListStatus.DROPPED
            ) {
                status = MediaListStatus.CURRENT
            }
        }
        _loading.onNext(true)

        disposables.add(
            mediaListRepository.updateMediaListProgress(
                MediaType.ANIME,
                mediaList.id ?: 0,
                status,
                repeat,
                targetProgress,
                null
            )
                .applyScheduler()
                .doFinally {
                    _loading.onNext(false)
                }
                .subscribe(
                    {
                        // do nothing
                    },
                    {
                        _error.onNext(it.getStringResource())
                    }
                )
        )
    }
}