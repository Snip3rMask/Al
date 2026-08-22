package msr.atsulab.app.data.response

import msr.atsulab.app.data.response.anilist.Activity

data class SocialData(
    val friendsActivities: List<Activity> = listOf(),
    val globalActivities: List<Activity> = listOf()
)