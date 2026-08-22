package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.type.ScoreFormat

data class MediaFilterComponent(
    val mediaFilter: MediaFilter,
    val mediaType: MediaType,
    val scoreFormat: ScoreFormat,
    val isUserList: Boolean,
    val hasBigList: Boolean,
    val isViewer: Boolean
)