package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.User

data class HomeAdapterComponent(
    val user: User? = null,
    val appSetting: AppSetting = AppSetting()
)
