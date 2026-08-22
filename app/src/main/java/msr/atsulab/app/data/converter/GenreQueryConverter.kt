package msr.atsulab.app.data.converter

import msr.atsulab.app.GenreQuery
import msr.atsulab.app.data.response.Genre

fun GenreQuery.Data.convert(): List<Genre> {
    return GenreCollection?.mapNotNull { Genre(it ?: "") } ?: listOf()
}