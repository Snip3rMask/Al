package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.MediaFormat
import msr.atsulab.app.type.MediaRankType
import msr.atsulab.app.type.MediaSeason


data class MediaRank(
    val id: Int = 0,
    val rank: Int = 0,
    val type: MediaRankType? = null,
    val format: MediaFormat? = null,
    val year: Int = 0,
    val season: MediaSeason? = null,
    val allTime: Boolean = false,
    val context: String = ""
)