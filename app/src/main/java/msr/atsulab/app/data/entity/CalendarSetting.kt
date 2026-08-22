package msr.atsulab.app.data.entity

data class CalendarSetting(
    var showOnlyWatchingAndPlanning: Boolean = true,
    var showOnlyCurrentSeason: Boolean = false,
    var showAdult: Boolean = false
)