package msr.atsulab.app.helper.enums

import msr.atsulab.app.helper.extensions.convertFromSnakeCase

enum class ListType {
    LINEAR,
    GRID,
    SIMPLIFIED,
    ALBUM
}

fun ListType.getString(): String {
    return name.convertFromSnakeCase()
}