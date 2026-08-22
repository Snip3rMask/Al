package msr.atsulab.app.data.converter

import msr.atsulab.app.data.response.VideoSearch
import msr.atsulab.app.data.response.youtube.VideoSearchResponse

fun VideoSearchResponse.convert(): VideoSearch {
    return VideoSearch(
        videoId = items?.firstOrNull()?.id?.videoId ?: ""
    )
}