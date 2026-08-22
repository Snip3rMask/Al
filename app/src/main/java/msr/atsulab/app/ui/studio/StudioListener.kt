package msr.atsulab.app.ui.studio

import msr.atsulab.app.data.response.anilist.Media

interface StudioListener {
    fun navigateToMedia(media: Media)
    fun navigateToStudioMedia()
}