package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.User

data class SocialAdapterComponent(
    val viewer: User? = null,
    val appSetting: AppSetting = AppSetting()
)