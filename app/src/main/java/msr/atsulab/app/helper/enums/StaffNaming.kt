package msr.atsulab.app.helper.enums

import msr.atsulab.app.helper.extensions.convertFromSnakeCase

enum class StaffNaming : Naming {
    FOLLOW_ANILIST,
    FIRST_MIDDLE_LAST,
    LAST_MIDDLE_FIRST,
    NATIVE
}

fun StaffNaming.getString(): String {
    return name.convertFromSnakeCase()
}