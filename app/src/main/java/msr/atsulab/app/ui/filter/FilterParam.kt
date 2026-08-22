package msr.atsulab.app.ui.filter

import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.type.ScoreFormat

data class FilterParam(
    val mediaFilter: MediaFilter,
    val mediaType: MediaType,
    val scoreFormat: ScoreFormat,
    val isUserList: Boolean,
    val hasBigList: Boolean,
    val isCurrentUser: Boolean
)