package msr.atsulab.app.helper.pojo

import msr.atsulab.app.data.response.anilist.*
import msr.atsulab.app.helper.enums.SearchCategory

data class SearchItem(
    val media: Media = Media(),
    val character: Character = Character(),
    val staff: Staff = Staff(),
    val studio: Studio = Studio(),
    val user: User = User(),
    val searchCategory: SearchCategory = SearchCategory.ANIME
)