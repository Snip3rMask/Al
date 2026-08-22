package msr.atsulab.app.data.converter

import msr.atsulab.app.ActivityQuery
import msr.atsulab.app.data.response.anilist.Activity
import msr.atsulab.app.data.response.anilist.ListActivity
import msr.atsulab.app.data.response.anilist.MessageActivity
import msr.atsulab.app.data.response.anilist.TextActivity

fun ActivityQuery.Data.convert(): Activity {
    return when (Activity?.__typename) {
        "TextActivity" -> {
            Activity?.onTextActivity?.convert() ?: TextActivity()
        }
        "ListActivity" -> {
            Activity?.onListActivity?.convert() ?: ListActivity()
        }
        "MessageActivity" -> {
            Activity?.onMessageActivity?.convert() ?: MessageActivity()
        }
        else -> TextActivity()
    }
}