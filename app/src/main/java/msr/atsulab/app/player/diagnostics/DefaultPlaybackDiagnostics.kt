package msr.atsulab.app.player.diagnostics

class DefaultPlaybackDiagnostics(
    private val enabled: Boolean,
    private val log: (priority: Int, tag: String, message: String, error: Throwable?) -> Unit
) : PlaybackDiagnostics {

    override fun onEpisodeProviderSkipped(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        reason: String
    ) {
        info("episode skipped provider=$providerId anime=$aniListId index=$providerIndex reason=$reason")
    }

    override fun onEpisodeProviderFailed(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        error: Throwable
    ) {
        warning("episode failed provider=$providerId anime=$aniListId index=$providerIndex", error)
    }

    override fun onSourceProviderSkipped(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        reason: String
    ) {
        info("source skipped provider=$providerId playback=$playbackId index=$providerIndex reason=$reason")
    }

    override fun onSourceProviderFailed(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        error: Throwable
    ) {
        warning("source failed provider=$providerId playback=$playbackId index=$providerIndex", error)
    }

    override fun onSourceMappingInvalid(aniListId: String) {
        warning("invalid source mapping removed anime=$aniListId", null)
    }

    private fun info(message: String) {
        if (enabled) log(INFO_PRIORITY, TAG, message, null)
    }

    private fun warning(message: String, error: Throwable?) {
        if (enabled) log(WARNING_PRIORITY, TAG, message, error)
    }

    private companion object {
        const val TAG = "AtsuLabPlayback"
        const val INFO_PRIORITY = 3
        const val WARNING_PRIORITY = 5
    }
}
