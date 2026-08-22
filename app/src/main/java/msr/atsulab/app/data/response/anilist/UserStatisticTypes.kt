package msr.atsulab.app.data.response.anilist

data class UserStatisticTypes(
    val anime: UserStatistics = UserStatistics(),
    val manga: UserStatistics = UserStatistics()
)