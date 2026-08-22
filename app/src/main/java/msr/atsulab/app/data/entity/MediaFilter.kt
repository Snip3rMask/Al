package msr.atsulab.app.data.entity

import msr.atsulab.app.data.response.anilist.MediaTag
import msr.atsulab.app.helper.enums.Country
import msr.atsulab.app.helper.enums.OtherLink
import msr.atsulab.app.helper.enums.Sort
import msr.atsulab.app.type.MediaFormat
import msr.atsulab.app.type.MediaSeason
import msr.atsulab.app.type.MediaSource
import msr.atsulab.app.type.MediaStatus
import msr.atsulab.app.type.UserTitleLanguage

data class MediaFilter(
    var persistFilter: Boolean = false,
    var sort: Sort = Sort.FOLLOW_LIST_SETTINGS,
    var titleLanguage: UserTitleLanguage = UserTitleLanguage.ROMAJI,
    var orderByDescending: Boolean = true,
    var mediaFormats: List<MediaFormat> = listOf(),
    var mediaStatuses: List<MediaStatus> = listOf(),
    var mediaSources: List<MediaSource> = listOf(),
    var countries: List<Country> = listOf(),
    var mediaSeasons: List<MediaSeason> = listOf(),
    var seasonYear: Int? = null,
    var minYear: Int? = null,
    var maxYear: Int? = null,
    var minEpisodes: Int? = null,
    var maxEpisodes: Int? = null,
    var minDuration: Int? = null,
    var maxDuration: Int? = null,
    var minAverageScore: Int? = null,
    var maxAverageScore: Int? = null,
    var minPopularity: Int? = null,
    var maxPopularity: Int? = null,
    var streamingOn: List<OtherLink> = listOf(),
    var includedGenres: List<String> = listOf(),
    var excludedGenres: List<String> = listOf(),
    var includedTags: List<MediaTag> = listOf(),
    var excludedTags: List<MediaTag> = listOf(),
    var minTagPercentage: Int = DEFAULT_MINIMUM_TAG_PERCENTAGE,
    var minUserScore: Int? = null,
    var maxUserScore: Int? = null,
    var minUserStartYear: Int? = null,
    var maxUserStartYear: Int? = null,
    var minUserCompletedYear: Int? = null,
    var maxUserCompletedYear: Int? = null,
    var minUserPriority: Int? = null,
    var maxUserPriority: Int? = null,
    var isDoujin: Boolean? = null,
    var onList: Boolean? = null
) {
    companion object {
        const val DEFAULT_MINIMUM_TAG_PERCENTAGE = 18
    }
}