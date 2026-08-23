package msr.atsulab.app.player.diagnostics

interface PlaybackDiagnostics {
    fun onEpisodeProviderSkipped(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        reason: String
    )

    fun onEpisodeProviderFailed(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        error: Throwable
    )

    fun onSourceProviderSkipped(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        reason: String
    )

    fun onSourceProviderFailed(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        error: Throwable
    )

    fun onSourceMappingInvalid(aniListId: String)
}

object NoOpPlaybackDiagnostics : PlaybackDiagnostics {
    override fun onEpisodeProviderSkipped(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        reason: String
    ) = Unit

    override fun onEpisodeProviderFailed(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        error: Throwable
    ) = Unit

    override fun onSourceProviderSkipped(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        reason: String
    ) = Unit

    override fun onSourceProviderFailed(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        error: Throwable
    ) = Unit

    override fun onSourceMappingInvalid(aniListId: String) = Unit
}
