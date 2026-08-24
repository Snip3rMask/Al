package msr.atsulab.app.player.mapper

import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.MediaCoverImage
import msr.atsulab.app.data.response.anilist.MediaTitle
import msr.atsulab.app.helper.enums.MediaNaming
import msr.atsulab.app.type.MediaStatus
import msr.atsulab.app.type.MediaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PlaybackAnimeMapperTest {

    @Test
    fun `maps anime media using app naming and image preferences`() {
        val appSetting = AppSetting(
            japaneseMediaNaming = MediaNaming.ENGLISH,
            useHighestQualityImage = true
        )
        val media = createMedia().copy(status = MediaStatus.RELEASING)

        val anime = media.toPlaybackAnime(appSetting)

        assertEquals(21, anime.aniListId)
        assertEquals(12345, anime.malId)
        assertEquals("English Title", anime.title)
        assertEquals(listOf("Romaji Title", "Native Title", "User Preferred"), anime.alternativeTitles)
        assertEquals("https://example.com/cover-extra-large.webp", anime.coverImageUrl)
        assertEquals("https://example.com/banner.webp", anime.bannerImageUrl)
        assertEquals(12, anime.totalEpisodes)
        assertEquals(24, anime.episodeDurationMinutes)
        assertEquals(2026, anime.releaseYear)
        assertEquals(PlaybackAnime.ReleaseStatus.RELEASING, anime.releaseStatus)
        assertEquals("JP", anime.countryOfOrigin)
        assertEquals(mapOf("mal" to "12345"), anime.externalIds)
    }

    @Test
    fun `uses normal cover quality by default`() {
        val media = createMedia()

        val anime = media.toPlaybackAnime(AppSetting())

        assertEquals("https://example.com/cover-large.webp", anime.coverImageUrl)
    }

    @Test
    fun `rejects manga media`() {
        val manga = createMedia(type = MediaType.MANGA)

        assertThrows(IllegalArgumentException::class.java) {
            manga.toPlaybackAnime(AppSetting())
        }
    }

    @Test
    fun `maps unknown status with safe defaults`() {
        val media = Media(idAniList = 31)

        val anime = media.toPlaybackAnime(AppSetting())

        assertEquals("user preferred", anime.title)
        assertEquals(PlaybackAnime.ReleaseStatus.UNKNOWN, anime.releaseStatus)
        assertEquals(null, anime.totalEpisodes)
        assertEquals(null, anime.episodeDurationMinutes)
    }

    private fun createMedia(type: MediaType = MediaType.ANIME): Media {
        return Media(
            idAniList = 21,
            idMal = 12345,
            title = MediaTitle(
                romaji = "Romaji Title",
                english = "English Title",
                native = "Native Title",
                userPreferred = "User Preferred"
            ),
            type = type,
            episodes = 12,
            duration = 24,
            seasonYear = 2026,
            countryOfOrigin = "JP",
            coverImage = MediaCoverImage(
                extraLarge = "https://example.com/cover-extra-large.webp",
                large = "https://example.com/cover-large.webp"
            ),
            bannerImage = "https://example.com/banner.webp"
        )
    }
}
