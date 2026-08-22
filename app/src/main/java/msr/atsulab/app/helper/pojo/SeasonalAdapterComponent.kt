package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.helper.enums.ListType

data class SeasonalAdapterComponent(
    val listType: ListType = ListType.LINEAR,
    val appSetting: AppSetting = AppSetting()
)