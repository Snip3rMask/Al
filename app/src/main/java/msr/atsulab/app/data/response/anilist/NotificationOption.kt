package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.NotificationType


data class NotificationOption(
    val type: NotificationType? = null,
    var enabled: Boolean = false
)