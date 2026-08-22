package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.response.anilist.Character
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.Staff
import msr.atsulab.app.data.response.anilist.Studio
import msr.atsulab.app.helper.enums.Favorite

data class FavoriteItem(
    val anime: Media? = null,
    val manga: Media? = null,
    val character: Character? = null,
    val staff: Staff? = null,
    val studio: Studio? = null,
    val favorite: Favorite
)