package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.entity.AppSetting

data class ReviewAdapterComponent(
    val appSetting: AppSetting = AppSetting(),
    val isMediaReview: Boolean = true,
    val isUserReview: Boolean = true
)
