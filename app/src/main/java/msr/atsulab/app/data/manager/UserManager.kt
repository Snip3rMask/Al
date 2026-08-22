package msr.atsulab.app.data.manager

import android.net.Uri
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.entity.CalendarSetting
import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.data.response.anilist.User
import msr.atsulab.app.data.entity.ListStyle
import msr.atsulab.app.data.response.anilist.MediaListCollection
import msr.atsulab.app.helper.pojo.NullableItem
import msr.atsulab.app.helper.pojo.SaveItem
import io.reactivex.rxjava3.core.Observable

interface UserManager {
    var bearerToken: String?
    val isAuthenticated: Boolean
    var isLoggedInAsGuest: Boolean

    var animeListStyle: ListStyle
    var mangaListStyle: ListStyle
    var animeFilter: MediaFilter
    var mangaFilter: MediaFilter
    var appSetting: AppSetting
    var calendarSetting: CalendarSetting

    val animeListBackground: Observable<NullableItem<Uri>>
    val mangaListBackground: Observable<NullableItem<Uri>>
    fun saveAnimeListBackground(uri: Uri?): Observable<Unit>
    fun saveMangaListBackground(uri: Uri?): Observable<Unit>

    var viewerData: SaveItem<User>?
    var followingCount: Int?
    var followersCount: Int?
    var animeListEntryCount: Int?
    var mangaListEntryCount: Int?

    var animeList: SaveItem<MediaListCollection>?
    var mangaList: SaveItem<MediaListCollection>?

    var lastNotificationId: Int?

    var lastAnnouncementId: String?
}