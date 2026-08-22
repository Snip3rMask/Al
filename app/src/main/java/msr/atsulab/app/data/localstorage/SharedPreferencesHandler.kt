package msr.atsulab.app.data.localstorage

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.entity.CalendarSetting
import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.data.entity.ListStyle
import msr.atsulab.app.data.response.SpotifyAccessToken
import msr.atsulab.app.helper.enums.ListType

interface SharedPreferencesHandler {
    var bearerToken: String?
    var guestLogin: Boolean?
    var animeListStyle: ListStyle?
    var mangaListStyle: ListStyle?
    var animeFilter: MediaFilter?
    var mangaFilter: MediaFilter?
    var appSetting: AppSetting?
    var calendarSetting: CalendarSetting?
    var followingCount: Int?
    var followersCount: Int?
    var animeListEntryCount: Int?
    var mangaListEntryCount: Int?
    var othersListType: ListType?
    var lastNotificationId: Int?
    var lastAnnouncementId: String?
    var spotifyAccessToken: SpotifyAccessToken?
    var spotifyAccessTokenLastRetrieve: Long?
}