package msr.atsulab.app.player.ui

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

class PlayerDebugStartupProvider : ContentProvider() {

    override fun attachInfo(context: android.content.Context?, info: android.content.pm.ProviderInfo?) {
        if (context != null) {
            PlayerDebugCrashRecorder.install(context)
            PlayerDebugCrashRecorder.breadcrumb(context, "startup_provider_attached")
        }
        super.attachInfo(context, info)
    }

    override fun onCreate(): Boolean = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int = 0
}
