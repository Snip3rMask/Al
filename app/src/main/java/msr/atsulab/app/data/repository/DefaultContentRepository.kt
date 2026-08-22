package msr.atsulab.app.data.repository

import msr.atsulab.app.data.datasource.ContentDataSource
import msr.atsulab.app.data.converter.convert
import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.data.manager.ContentManager
import msr.atsulab.app.data.response.HomeData
import msr.atsulab.app.helper.enums.Source
import msr.atsulab.app.helper.extensions.moreThanADay
import msr.atsulab.app.data.response.Genre
import msr.atsulab.app.data.response.anilist.*
import msr.atsulab.app.helper.enums.ReviewSort
import msr.atsulab.app.helper.enums.Sort
import msr.atsulab.app.helper.pojo.SaveItem
import msr.atsulab.app.helper.utils.NotInStorageException
import msr.atsulab.app.type.MediaSeason
import msr.atsulab.app.type.MediaType
import msr.atsulab.app.type.ReviewRating
import msr.atsulab.app.type.UserTitleLanguage
import io.reactivex.rxjava3.core.Observable

class DefaultContentRepository(
    private val contentDataSource: ContentDataSource,
    private val contentManager: ContentManager
) : BaseRepository(), ContentRepository {

    override fun getHomeData(source: Source?): Observable<HomeData> {
        return when(source) {
            Source.NETWORK -> getHomeDataFromNetwork()
            Source.CACHE -> getHomeDataFromCache()
            else -> {
                val savedItem = contentManager.homeData
                if (savedItem == null || savedItem.saveTime.moreThanADay()) {
                    getHomeDataFromNetwork()
                } else {
                    Observable.just(savedItem.data)
                }
            }
        }
    }

    private fun getHomeDataFromNetwork(): Observable<HomeData> {
        return contentDataSource.getHomeQuery().map {
            val newHomeData = it.data?.convert() ?: HomeData()
            contentManager.homeData = SaveItem(newHomeData)
            newHomeData
        }
    }

    private fun getHomeDataFromCache(): Observable<HomeData> {
        val savedItem = contentManager.homeData?.data
        return if (savedItem != null) Observable.just(savedItem) else Observable.error(NotInStorageException())
    }

    override fun getGenres(): Observable<List<Genre>> {
        val savedItem = contentManager.genres
        return if (savedItem == null || savedItem.saveTime.moreThanADay()) {
            contentDataSource.getGenres()
                .map {
                    val newGenres = it.data?.convert() ?: listOf()
                    contentManager.genres = SaveItem(newGenres)
                    newGenres
                }
                .onErrorReturn {
                    savedItem?.data ?: listOf()
                }

        } else {
            Observable.just(savedItem.data)
        }
    }

    override fun getTags(): Observable<List<MediaTag>> {
        val savedItem = contentManager.tags
        return if (savedItem == null || savedItem.saveTime.moreThanADay()) {
            contentDataSource.getTags()
                .map {
                    val newTags = it.data?.convert() ?: listOf()
                    contentManager.tags = SaveItem(newTags)
                    newTags
                }
                .onErrorReturn {
                    savedItem?.data ?: listOf()
                }
        } else {
            Observable.just(savedItem.data)
        }
    }

    override fun searchMedia(
        searchQuery: String,
        type: MediaType,
        mediaFilter: MediaFilter?,
        page: Int
    ): Observable<Page<Media>> {
        return contentDataSource.searchMedia(searchQuery, type, mediaFilter, page).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun searchCharacter(searchQuery: String, page: Int): Observable<Page<Character>> {
        return contentDataSource.searchCharacter(searchQuery, page).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun searchStaff(searchQuery: String, page: Int): Observable<Page<Staff>> {
        return contentDataSource.searchStaff(searchQuery, page).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun searchStudio(searchQuery: String, page: Int): Observable<Page<Studio>> {
        return contentDataSource.searchStudio(searchQuery, page).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun searchUser(searchQuery: String, page: Int): Observable<Page<User>> {
        return contentDataSource.searchUser(searchQuery, page).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun getSeasonal(
        page: Int,
        year: Int,
        season: MediaSeason,
        sort: Sort,
        titleLanguage: UserTitleLanguage,
        orderByDescending: Boolean,
        onlyShowOnList: Boolean?,
        showAdult: Boolean
    ): Observable<Page<Media>> {
        return contentDataSource.getSeasonal(page, year, season, sort, titleLanguage, orderByDescending, onlyShowOnList, showAdult).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun getAiringSchedule(
        page: Int,
        airingAtGreater: Int,
        airingAtLesser: Int
    ): Observable<Page<AiringSchedule>> {
        return contentDataSource.getAiringSchedule(page, airingAtGreater, airingAtLesser).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun getReviews(
        mediaId: Int?,
        userId: Int?,
        mediaType: MediaType?,
        sort: ReviewSort,
        page: Int
    ): Observable<Page<Review>> {
        return contentDataSource.getReviews(mediaId, userId, mediaType, sort, page).map {
            it.data?.convert() ?: Page()
        }
    }

    override fun rateReview(id: Int, rating: ReviewRating): Observable<Review> {
        return contentDataSource.rateReview(id, rating).map {
            it.data?.convert() ?: Review()
        }
    }
}