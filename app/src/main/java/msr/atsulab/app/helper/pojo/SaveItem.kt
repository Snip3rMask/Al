package msr.atsulab.app.helper.pojo

import msr.atsulab.app.helper.utils.TimeUtil

class SaveItem<T>(
    val data: T,
    var saveTime: Long = TimeUtil.getCurrentTimeInMillis()
) {
}