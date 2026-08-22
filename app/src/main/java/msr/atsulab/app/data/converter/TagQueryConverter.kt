package msr.atsulab.app.data.converter

import msr.atsulab.app.TagQuery
import msr.atsulab.app.data.response.anilist.MediaTag

fun TagQuery.Data.convert(): List<MediaTag> {
    return MediaTagCollection?.mapNotNull {
        MediaTag(
            id = it?.id ?: 0,
            name = it?.name ?: "",
            description = it?.description ?: "",
            category = it?.category ?: "",
            rank = it?.rank ?: 0,
            isGeneralSpoiler = it?.isGeneralSpoiler ?: false,
            isMediaSpoiler = it?.isMediaSpoiler ?: false,
            isAdult = it?.isAdult ?: false
        )
    } ?: listOf()
}