package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.MediaListStatus


data class StatusDistribution(
    val status: MediaListStatus? = null,
    val amount: Int = 0
)