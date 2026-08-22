package msr.atsulab.app.data.response

import msr.atsulab.app.data.response.anilist.Notification
import msr.atsulab.app.data.response.anilist.Page

data class NotificationData(
    val page: Page<Notification> = Page()
)