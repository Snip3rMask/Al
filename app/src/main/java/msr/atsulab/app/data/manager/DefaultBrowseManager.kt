package msr.atsulab.app.data.manager

import msr.atsulab.app.BuildConfig
import msr.atsulab.app.data.localstorage.SharedPreferencesHandler
import msr.atsulab.app.data.response.SpotifyAccessToken
import msr.atsulab.app.helper.enums.ListType

class DefaultBrowseManager(private val sharedPreferencesHandler: SharedPreferencesHandler) : BrowseManager {

    override var othersListType: ListType
        get() = sharedPreferencesHandler.othersListType ?: ListType.LINEAR
        set(value) { sharedPreferencesHandler.othersListType = value }

    override val youTubeApiKey: String
        get() = BuildConfig.YOUTUBE_API_KEY

    override val spotifyApiKey: String
        get() = BuildConfig.SPOTIFY_API_KEY

    override var spotifyAccessToken: SpotifyAccessToken
        get() = sharedPreferencesHandler.spotifyAccessToken ?: SpotifyAccessToken()
        set(value) { sharedPreferencesHandler.spotifyAccessToken = value }

    override var spotifyAccessTokenLastRetrieve: Long
        get() = sharedPreferencesHandler.spotifyAccessTokenLastRetrieve ?: 0
        set(value) { sharedPreferencesHandler.spotifyAccessTokenLastRetrieve = value }
}