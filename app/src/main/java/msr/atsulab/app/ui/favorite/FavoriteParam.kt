package msr.atsulab.app.ui.favorite

import msr.atsulab.app.helper.enums.Favorite

data class FavoriteParam(
    val userId: Int,
    val favorite: Favorite
)