package msr.atsulab.app.helper.enums

import msr.atsulab.app.helper.extensions.convertFromSnakeCase

enum class MediaNaming : Naming {
    FOLLOW_ANILIST,
    ENGLISH,
    ROMAJI,
    NATIVE
}

fun MediaNaming.getString(): String {
    return name.convertFromSnakeCase()
}