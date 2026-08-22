package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.type.NotificationType

data class MediaDeletionNotification(
    override val id: Int = 0,
    override val type: NotificationType = NotificationType.MEDIA_DELETION,
    val deletedMediaTitle: String = "",
    val context: String = "",
    val reason: String = "",
    override val createdAt: Int = 0
) : Notification {
    override fun getMessage(appSetting: AppSetting): String {
        return "${deletedMediaTitle}${context}"
    }
}