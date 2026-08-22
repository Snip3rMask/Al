package msr.atsulab.app.data.response

import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.Review

data class HomeData(
    val trendingAnime: List<Media> = listOf(),
    val trendingManga: List<Media> = listOf(),
    val newAnime: List<Media> = listOf(),
    val newManga: List<Media> = listOf()
)