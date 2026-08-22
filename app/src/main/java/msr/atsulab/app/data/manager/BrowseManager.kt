package msr.atsulab.app.data.manager

import msr.atsulab.app.data.response.SpotifyAccessToken
import msr.atsulab.app.helper.enums.ListType

interface BrowseManager {
    var othersListType: ListType
    val youTubeApiKey: String
    val spotifyApiKey: String
    var spotifyAccessToken: SpotifyAccessToken
    var spotifyAccessTokenLastRetrieve: Long
}