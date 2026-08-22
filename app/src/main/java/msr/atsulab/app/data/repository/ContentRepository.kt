package msr.atsulab.app.data.repository

import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.data.response.HomeData
import msr.atsulab.app.helper.enums.Source
import msr.atsulab.app.data.response.Genre
import msr.atsulab.app.data.response.anilist.*
import msr.atsulab.app.helper.enums.ReviewSort
import msr.atsulab.app.helper.enums.Sort
import msr.atsulab.app.type.MediaSeason
import msr.atsulab.app.type.MediaType
import msr.atsulab.app.type.ReviewRating
import msr.atsulab.app.type.UserTitleLanguage
import io.reactivex.rxjava3.core.Observable

interface ContentRepository {
    fun getHomeData(source: Source? = null): Observable<HomeData>
    fun getGenres(): Observable<List<Genre>>
    fun getTags(): Observable<List<MediaTag>>
    fun searchMedia(searchQuery: String, type: MediaType, mediaFilter: MediaFilter?, page: Int): Observable<Page<Media>>
    fun searchCharacter(searchQuery: String, page: Int): Observable<Page<Character>>
    fun searchStaff(searchQuery: String, page: Int): Observable<Page<Staff>>
    fun searchStudio(searchQuery: String, page: Int): Observable<Page<Studio>>
    fun searchUser(searchQuery: String, page: Int): Observable<Page<User>>
    fun getSeasonal(page: Int, year: Int, season: MediaSeason, sort: Sort, titleLanguage: UserTitleLanguage, orderByDescending: Boolean, onlyShowOnList: Boolean?, showAdult: Boolean): Observable<Page<Media>>
    fun getAiringSchedule(page: Int, airingAtGreater: Int, airingAtLesser: Int): Observable<Page<AiringSchedule>>
    fun getReviews(mediaId: Int?, userId: Int?, mediaType: MediaType?, sort: ReviewSort, page: Int): Observable<Page<Review>>
    fun rateReview(id: Int, rating: ReviewRating): Observable<Review>
}