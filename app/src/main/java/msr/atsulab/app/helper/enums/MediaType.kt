package msr.atsulab.app.helper.enums

import msr.atsulab.app.R

enum class MediaType {
    ANIME,
    MANGA
}

fun MediaType.getAniListMediaType(): msr.atsulab.app.type.MediaType {
    return when (this) {
        MediaType.ANIME -> msr.atsulab.app.type.MediaType.ANIME
        MediaType.MANGA -> msr.atsulab.app.type.MediaType.MANGA
    }
}

fun MediaType.getStringResource(): Int {
    return when (this) {
        MediaType.ANIME -> R.string.anime
        MediaType.MANGA -> R.string.manga
    }
}