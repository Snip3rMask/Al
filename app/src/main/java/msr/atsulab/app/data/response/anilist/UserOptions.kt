package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.UserStaffNameLanguage
import msr.atsulab.app.type.UserTitleLanguage


data class UserOptions(
    var titleLanguage: UserTitleLanguage? = null,
    var displayAdultContent: Boolean = false,
    var airingNotifications: Boolean = false,
    val notificationOptions: List<NotificationOption> = listOf(),
    val timezone: String? = null,
    var activityMergeTime: Int = 0,
    var staffNameLanguage: UserStaffNameLanguage? = null,
    var restrictMessagesToFollowing: Boolean = false,
    var disabledListActivity: List<ListActivityOption> = listOf()

)