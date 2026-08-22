package msr.atsulab.app.data.response.anilist

import msr.atsulab.app.type.ExternalLinkType


data class MediaExternalLink(
    val id: Int = 0,
    val url: String = "",
    val site: String = "",
    val siteId: Int = 0,
    val type: ExternalLinkType? = null,
    val language: String = "",
    val color: String = "",
    val icon: String = ""
) {
    fun getSiteNameWithLanguage(): String {
        return site + if (language.isNotBlank()) " (${language})" else ""
    }
}