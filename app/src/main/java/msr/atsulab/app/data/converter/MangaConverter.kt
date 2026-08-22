package msr.atsulab.app.data.converter

import msr.atsulab.app.data.response.Manga
import msr.atsulab.app.data.response.MangaSerialization
import msr.atsulab.app.data.response.mal.MangaResponse

fun MangaResponse.convert(): Manga {
    return Manga(
        malId = data?.malId ?: 0,
        title = data?.title ?: "",
        serializations = data?.serializations?.map {
            MangaSerialization(
                name = it.name ?: "",
                url = it.url ?: ""
            )
        } ?: listOf()
    )
}