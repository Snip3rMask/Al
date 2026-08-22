package msr.atsulab.app.data.converter

import msr.atsulab.app.data.response.TrackSearch
import msr.atsulab.app.data.response.spotify.TrackSearchResponse

fun TrackSearchResponse.convert(): TrackSearch {
    return TrackSearch(
        trackUrl = tracks?.items?.firstOrNull()?.externalUrls?.spotify ?: ""
    )
}