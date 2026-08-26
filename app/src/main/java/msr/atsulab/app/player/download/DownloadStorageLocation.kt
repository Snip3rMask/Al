package msr.atsulab.app.player.download

import msr.atsulab.app.R

enum class DownloadStorageLocation(val storageValue: String, val labelResId: Int) {
    INTERNAL("internal", R.string.download_storage_internal),
    EXTERNAL_APP("external_app", R.string.download_storage_external_app);

    companion object {
        fun from(storageValue: String?): DownloadStorageLocation {
            return values().firstOrNull { it.storageValue == storageValue } ?: INTERNAL
        }
    }
}
