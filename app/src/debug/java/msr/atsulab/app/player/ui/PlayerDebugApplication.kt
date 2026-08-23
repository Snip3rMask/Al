package msr.atsulab.app.player.ui

import android.content.Context
import msr.atsulab.app.ALchanApplication

class PlayerDebugApplication : ALchanApplication() {

    override fun attachBaseContext(base: Context) {
        PlayerDebugCrashRecorder.install(base)
        super.attachBaseContext(base)
    }
}
