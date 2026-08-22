package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.type.NotificationType

interface Notification {
    val id: Int
    val type: NotificationType
    val createdAt: Int
    fun getMessage(appSetting: AppSetting): String
}