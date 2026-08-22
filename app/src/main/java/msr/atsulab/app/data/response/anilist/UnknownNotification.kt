package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.type.NotificationType

data class UnknownNotification(
    override val id: Int = 0,
    override val type: NotificationType = NotificationType.UNKNOWN__,
    override val createdAt: Int = 0
) : Notification {
    override fun getMessage(appSetting: AppSetting): String {
        return ""
    }
}
