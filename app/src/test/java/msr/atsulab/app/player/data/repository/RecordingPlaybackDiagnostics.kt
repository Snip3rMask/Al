package msr.atsulab.app.player.data.repository

import msr.atsulab.app.player.diagnostics.PlaybackDiagnostics

class RecordingPlaybackDiagnostics : PlaybackDiagnostics {
    val skipped = mutableListOf<String>()
    val failures = mutableListOf<String>()
    val invalidMappings = mutableListOf<String>()

    override fun onEpisodeProviderSkipped(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        reason: String
    ) {
        skipped += "episode:$providerId:$providerIndex:$aniListId:$reason"
    }

    override fun onEpisodeProviderFailed(
        providerId: String,
        providerIndex: Int,
        aniListId: Int?,
        error: Throwable
    ) {
        failures += "episode:$providerId:$providerIndex:$aniListId:${error::class.simpleName}"
    }

    override fun onSourceProviderSkipped(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        reason: String
    ) {
        skipped += "source:$providerId:$providerIndex:$playbackId:$reason"
    }

    override fun onSourceProviderFailed(
        providerId: String,
        providerIndex: Int,
        playbackId: String,
        error: Throwable
    ) {
        failures += "source:$providerId:$providerIndex:$playbackId:${error::class.simpleName}"
    }

    override fun onSourceMappingInvalid(aniListId: String) {
        invalidMappings += aniListId
    }
}
