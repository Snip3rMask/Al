package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.data.entity.AppSetting

data class UserAvatar(
    val large: String = "",
    val medium: String = ""
) {
    fun getImageUrl(appSetting: AppSetting): String {
        return if (appSetting.useHighestQualityImage)
            large
        else
            medium
    }
}