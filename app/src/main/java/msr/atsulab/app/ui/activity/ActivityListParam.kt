package msr.atsulab.app.ui.activity

import msr.atsulab.app.helper.enums.ActivityListPage

data class ActivityListParam(
    val activityListPage: ActivityListPage,
    val userId: Int
)