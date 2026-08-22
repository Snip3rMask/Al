package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.CharacterRole


data class CharacterEdge(
    val node: Character = Character(),
    val role: CharacterRole? = null,
    val name: String = "",
    val voiceActorRoles: List<StaffRoleType> = listOf(),
    val media: List<Media> = listOf()
)