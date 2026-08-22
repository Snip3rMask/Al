package msr.atsulab.app.ui.staff.character

import msr.atsulab.app.data.response.anilist.Character
import msr.atsulab.app.data.response.anilist.Media

interface StaffCharacterListListener {
    fun navigateToCharacter(character: Character)
    fun navigateToMedia(media: Media)
}