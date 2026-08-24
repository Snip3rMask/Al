package msr.atsulab.app.player.mapper

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.player.domain.model.PlaybackAnime
import msr.atsulab.app.type.MediaStatus
import msr.atsulab.app.type.MediaType

fun Media.toPlaybackAnime(appSetting: AppSetting): PlaybackAnime {
    require(type == MediaType.ANIME) { "Playback is only available for anime" }

    val selectedTitle = getTitle(appSetting)
    return PlaybackAnime(
        aniListId = idAniList,
        malId = idMal?.takeIf { it > 0 },
        title = selectedTitle,
        alternativeTitles = listOf(
            title.romaji,
            title.english,
            title.native,
            title.userPreferred
        ).filter(String::isNotBlank).distinct() - selectedTitle,
        coverImageUrl = getCoverImage(appSetting),
        bannerImageUrl = bannerImage,
        totalEpisodes = episodes?.takeIf { it > 0 },
        episodeDurationMinutes = duration?.takeIf { it > 0 },
        releaseYear = seasonYear,
        releaseStatus = status.toPlaybackReleaseStatus(),
        isAdult = isAdult,
        countryOfOrigin = countryOfOrigin,
        externalIds = buildMap {
            idMal?.takeIf { it > 0 }?.let { malId -> put("mal", malId.toString()) }
        }
    )
}

fun Media.toPlaybackStart(appSetting: AppSetting): Result<PlaybackAnime> {
    return runCatching {
        require(type == MediaType.ANIME) { "Playback is only available for anime" }
        require(idAniList > 0) { "A valid AniList anime ID is required" }
        require(status != MediaStatus.NOT_YET_RELEASED) { "This anime has not been released yet" }

        val anime = toPlaybackAnime(appSetting)
        require(anime.title.isNotBlank()) { "A valid anime title is required" }
        anime
    }
}

private fun MediaStatus?.toPlaybackReleaseStatus(): PlaybackAnime.ReleaseStatus {
    return when (this) {
        MediaStatus.FINISHED -> PlaybackAnime.ReleaseStatus.FINISHED
        MediaStatus.RELEASING -> PlaybackAnime.ReleaseStatus.RELEASING
        MediaStatus.NOT_YET_RELEASED -> PlaybackAnime.ReleaseStatus.NOT_YET_RELEASED
        MediaStatus.CANCELLED -> PlaybackAnime.ReleaseStatus.CANCELLED
        MediaStatus.HIATUS -> PlaybackAnime.ReleaseStatus.HIATUS
        else -> PlaybackAnime.ReleaseStatus.UNKNOWN
    }
}
