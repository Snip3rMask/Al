package msr.atsulab.app.data.repository

import android.net.Uri
import msr.atsulab.app.data.entity.ListStyle
import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.data.response.anilist.FuzzyDate
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.MediaList
import msr.atsulab.app.data.response.anilist.MediaListCollection
import msr.atsulab.app.data.response.anilist.User
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.helper.enums.Source
import msr.atsulab.app.helper.pojo.NullableItem
import msr.atsulab.app.type.MediaListStatus
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface MediaListRepository {
    val defaultAnimeList: List<String>
    val defaultAnimeListSplitCompletedSectionByFormat: List<String>
    val defaultMangaList: List<String>
    val defaultMangaListSplitCompletedSectionByFormat: List<String>
    val refreshMediaListTrigger: Observable<Pair<MediaType, MediaList?>>
    val releasingTodayTrigger: Observable<Unit>

    fun getMediaListCollection(source: Source = Source.NETWORK, user: User, mediaType: MediaType): Observable<MediaListCollection>
    fun hasBigList(user: User, mediaType: MediaType) : Observable<Boolean>
    fun updateCacheMediaList(mediaType: MediaType, mediaListCollection: MediaListCollection)
    fun getMediaWithMediaList(mediaId: Int): Observable<Media>
    fun updateMediaListEntry(
        mediaType: MediaType,
        id: Int?,
        mediaId: Int?,
        status: MediaListStatus,
        score: Double,
        progress: Int,
        progressVolumes: Int?,
        repeat: Int,
        priority: Int,
        isPrivate: Boolean,
        notes: String,
        hiddenFromStatusLists: Boolean,
        customLists: List<String>?,
        advancedScores: List<Double>?,
        startedAt: FuzzyDate?,
        completedAt: FuzzyDate?
    ): Observable<MediaList>
    fun deleteMediaListEntry(mediaType: MediaType, id: Int): Completable
    fun updateMediaListScore(mediaType: MediaType, id: Int, score: Double, advancedScores: List<Double>?): Observable<MediaList>
    fun updateMediaListProgress(
        mediaType: MediaType,
        id: Int,
        status: MediaListStatus?,
        repeat: Int?,
        progress: Int?,
        progressVolumes: Int?
    ): Observable<MediaList>
    fun updateMediaListStatus(mediaType: MediaType, mediaId: Int, status: MediaListStatus): Observable<MediaList>

    fun getListStyle(mediaType: MediaType): Observable<ListStyle>
    fun setListStyle(mediaType: MediaType, newListStyle: ListStyle)
    fun getListBackground(mediaType: MediaType): Observable<NullableItem<Uri>>
    fun setListBackground(mediaType: MediaType, newUri: Uri?): Observable<Unit>
    fun getMediaFilter(mediaType: MediaType): Observable<MediaFilter>
    fun setMediaFilter(mediaType: MediaType, newMediaFilter: MediaFilter)
    fun triggerReleasingToday()
}