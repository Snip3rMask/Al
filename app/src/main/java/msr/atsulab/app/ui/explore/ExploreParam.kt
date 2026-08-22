package msr.atsulab.app.ui.explore

import msr.atsulab.app.data.entity.MediaFilter
import msr.atsulab.app.helper.enums.SearchCategory

data class ExploreParam(
    val searchCategory: SearchCategory,
    val mediaFilter: MediaFilter?
)
