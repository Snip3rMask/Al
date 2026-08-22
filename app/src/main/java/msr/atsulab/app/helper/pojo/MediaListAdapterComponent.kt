package msr.atsulab.app.helper.pojo

import android.net.Uri
import msr.atsulab.app.data.entity.AppSetting
import msr.atsulab.app.data.entity.ListStyle
import msr.atsulab.app.data.response.anilist.MediaListOptions

data class MediaListAdapterComponent(
    var isViewer: Boolean = false,
    var listStyle: ListStyle = ListStyle(),
    var appSetting: AppSetting = AppSetting(),
    var mediaListOptions: MediaListOptions = MediaListOptions(),
    var backgroundUri: Uri? = null
)