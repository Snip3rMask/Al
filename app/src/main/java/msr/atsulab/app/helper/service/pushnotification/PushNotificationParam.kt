package msr.atsulab.app.helper.service.pushnotification

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Notification

data class PushNotificationParam(
    val notifications: List<Notification>,
    val unreadNotificationCount: Int,
    val appSetting: AppSetting,
    val lastNotificationId: Int
)
