package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.type.MediaSort

data class StudioMediaListAdapterComponent(
    val appSetting: AppSetting = AppSetting(),
    val mediaSort: MediaSort = MediaSort.POPULARITY_DESC
)