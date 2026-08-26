package msr.atsulab.app.player.download

data class DownloadRequest(
    val aniListId: Int,
    val episodeId: String,
    val displayName: String,
    val url: String,
    val quality: String = DEFAULT_QUALITY,
    val referer: String = ""
) {
    val fileName: String
        get() = "${displayName.sanitized()} - ${episodeId.sanitized()}.mp4"

    private fun String.sanitized(): String {
        return map { character ->
            if (character.isLetterOrDigit() || character in " -_.") character else ' '
        }.joinToString("").trim().replace(Regex("\\s+"), " ").ifEmpty { "download" }
    }

    companion object {
        const val DEFAULT_QUALITY = "Auto"
    }
}

data class HlsVariant(
    val url: String,
    val label: String,
    val bandwidth: Int = 0,
    val width: Int? = null,
    val height: Int? = null
)

data class HlsMediaPlaylist(
    val segmentUrls: List<String>,
    val initializationUrl: String? = null
) {
    val isEmpty: Boolean
        get() = segmentUrls.isEmpty()
}

fun interface DownloadCancelToken {
    fun isCancelled(): Boolean

    companion object {
        val NEVER = DownloadCancelToken { false }
    }
}

fun interface DownloadProgressListener {
    fun onProgress(doneSegments: Int, totalSegments: Int)
}
