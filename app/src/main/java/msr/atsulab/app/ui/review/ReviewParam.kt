package msr.atsulab.app.ui.review

import msr.atsulab.app.data.response.anilist.Media

data class ReviewParam(
    val media: Media?,
    val userId: Int?
)
