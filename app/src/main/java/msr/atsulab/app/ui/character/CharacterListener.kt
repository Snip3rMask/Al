package msr.atsulab.app.ui.character

import msr.atsulab.app.data.response.anilist.Media
import msr.atsulab.app.data.response.anilist.Staff

interface CharacterListener {
    fun toggleShowMore(shouldShowMore: Boolean)
    fun navigateToStaff(staff: Staff)
    fun showStaffMedia(staff: Staff)
    fun navigateToCharacterMedia()

    val characterMediaListener: CharacterMediaListener

    interface CharacterMediaListener {
        fun navigateToMedia(media: Media)
    }
}