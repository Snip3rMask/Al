package msr.atsulab.app.helper.enums

import msr.atsulab.app.helper.extensions.convertFromSnakeCase

enum class Country(val iso: String) {
    JAPAN("JP"),
    SOUTH_KOREA("KR"),
    CHINA("CN"),
    TAIWAN("TW")
}

fun Country.getString(): String {
    return this.name.convertFromSnakeCase(true)
}
