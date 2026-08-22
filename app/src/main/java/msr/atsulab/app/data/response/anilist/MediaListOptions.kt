package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.ScoreFormat


data class MediaListOptions(
    var scoreFormat: ScoreFormat? = null,
    var rowOrder: String = "",
    val animeList: MediaListTypeOptions = MediaListTypeOptions(),
    val mangaList: MediaListTypeOptions = MediaListTypeOptions()
)