package msr.atsulab.app.data.converter

import msr.atsulab.app.data.response.Announcement
import msr.atsulab.app.data.response.github.AnnouncementResponse

fun AnnouncementResponse.convert(): Announcement {
    return Announcement(
        id = id ?: "",
        fromDate = fromDate ?: "",
        untilDate = untilDate?: "",
        message = message ?: "",
        appVersion = appVersion?.toIntOrNull() ?: 0,
        requiredUpdate = requiredUpdate == "1"
    )
}