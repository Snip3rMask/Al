package msr.atsulab.app.ui.media.themes

import msr.atsulab.app.data.response.AnimeTheme
import msr.atsulab.app.data.response.AnimeThemeEntry
import msr.atsulab.app.data.response.anilist.Media

data class BottomSheetMediaThemesParam(
    val media: Media,
    val animeTheme: AnimeTheme,
    val animeThemeEntry: AnimeThemeEntry?
)