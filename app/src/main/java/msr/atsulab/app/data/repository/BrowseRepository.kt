package msr.atsulab.app.data.repository

import msr.atsulab.app.data.response.Anime
import msr.atsulab.app.data.response.Manga
import msr.atsulab.app.data.response.TrackSearch
import msr.atsulab.app.data.response.VideoSearch
import msr.atsulab.app.data.response.anilist.*
import msr.atsulab.app.data.response.anilist.Character
import msr.atsulab.app.data.response.anilist.CharacterEdge
import msr.atsulab.app.data.response.anilist.ListActivity
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.MediaList
import msr.atsulab.app.data.response.anilist.Page
import msr.atsulab.app.data.response.anilist.PageInfo
import msr.atsulab.app.data.response.anilist.Staff
import msr.atsulab.app.data.response.anilist.StaffEdge
import msr.atsulab.app.data.response.anilist.Studio
import msr.atsulab.app.data.response.anilist.User
import msr.atsulab.app.helper.enums.ListType
import msr.atsulab.app.type.*
import io.reactivex.rxjava3.core.Observable

interface BrowseRepository {
    fun getUser(id: Int? = null, name: String? = null, sort: List<UserStatisticsSort> = listOf(UserStatisticsSort.COUNT_DESC)): Observable<User>
    fun getOthersListType(): Observable<ListType>
    fun updateOthersListType(newListType: ListType)
    fun getMedia(id: Int): Observable<Media>
    fun getMediaCharacters(id: Int, page: Int, language: StaffLanguage): Observable<Pair<PageInfo, List<CharacterEdge>>>
    fun getMediaStaff(id: Int, page: Int): Observable<Pair<PageInfo, List<StaffEdge>>>
    fun getMediaFollowingMediaList(id: Int, page: Int): Observable<Page<MediaList>>
    fun getMediaActivity(id: Int, page: Int): Observable<Page<ListActivity>>
    fun getCharacter(id: Int, page: Int, sort: List<MediaSort> = listOf(MediaSort.POPULARITY_DESC), type: MediaType? = null, onList: Boolean? = null): Observable<Character>
    fun getStaff(
        id: Int,
        page: Int,
        staffMediaSort: List<MediaSort> = listOf(MediaSort.POPULARITY_DESC),
        characterSort: List<CharacterSort> = listOf(CharacterSort.FAVOURITES_DESC),
        characterMediaSort: List<MediaSort> = listOf(MediaSort.POPULARITY_DESC),
        onList: Boolean? = null
    ): Observable<Staff>
    fun getStudio(id: Int, page: Int, sort: List<MediaSort> = listOf(MediaSort.POPULARITY_DESC), onList: Boolean? = null): Observable<Studio>

    fun getMangaDetails(malId: Int): Observable<Manga>
    fun getAnimeDetails(malId: Int): Observable<Anime>
    fun getYouTubeVideo(searchQuery: String): Observable<VideoSearch>
    fun getSpotifyTrack(searchQuery: String): Observable<TrackSearch>
}