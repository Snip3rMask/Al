package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.MediaListStatus


data class ListActivityOption(
    val disabled: Boolean = false,
    val type: MediaListStatus? = null
)