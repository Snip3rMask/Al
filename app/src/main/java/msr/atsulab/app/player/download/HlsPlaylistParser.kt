package msr.atsulab.app.player.download

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object HlsPlaylistParser {

    fun parseMaster(content: String, baseUrl: String): List<HlsVariant> {
        requireContent(content)
        val variants = mutableListOf<HlsVariant>()
        var pendingAttributes: String? = null

        content.lines().map(::trimLine).forEach { line ->
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                pendingAttributes = line.substringAfter(':', "")
            } else if (pendingAttributes != null && isMediaUrl(line)) {
                val attributes = pendingAttributes.orEmpty()
                variants += HlsVariant(
                    url = absolute(line, baseUrl),
                    label = labelFor(attributes),
                    bandwidth = numericValue(attributes, "BANDWIDTH"),
                    width = resolution(attributes).first,
                    height = resolution(attributes).second
                )
                pendingAttributes = null
            }
        }
        return variants
    }

    fun parseMedia(content: String, baseUrl: String): HlsMediaPlaylist {
        requireContent(content)
        val segments = mutableListOf<String>()
        var initializationUrl: String? = null

        content.lines().map(::trimLine).forEach { line ->
            when {
                line.startsWith("#EXT-X-KEY") && isEncrypted(line) -> {
                    throw IllegalArgumentException("Encrypted HLS streams are not supported")
                }
                line.startsWith("#EXT-X-MAP") -> initializationUrl = uriAttribute(line)?.let { absolute(it, baseUrl) }
                isMediaUrl(line) -> segments += absolute(line, baseUrl)
            }
        }
        return HlsMediaPlaylist(segments, initializationUrl)
    }

    fun isMasterPlaylist(content: String): Boolean {
        return content.lines().any { trimLine(it).startsWith("#EXT-X-STREAM-INF") }
    }

    private fun requireContent(content: String) {
        require(content.isNotBlank()) { "Empty HLS playlist" }
        require(content.contains("#EXTM3U")) { "Response is not an HLS playlist" }
    }

    private fun isEncrypted(line: String): Boolean {
        return urilessMethod(line).uppercase() != "NONE"
    }

    private fun urilessMethod(line: String): String {
        val start = line.indexOf("METHOD=", ignoreCase = true)
        if (start < 0) return "AES-128"
        val valueStart = start + "METHOD=".length
        val valueEnd = line.indexOf(',', valueStart).let { if (it < 0) line.length else it }
        return line.substring(valueStart, valueEnd).trim('"', '\'')
    }

    private fun uriAttribute(line: String): String? {
        val key = "URI=\""
        val start = line.indexOf(key, ignoreCase = true)
        if (start < 0) return null
        val valueStart = start + key.length
        val valueEnd = line.indexOf('"', valueStart)
        return line.substring(valueStart, valueEnd).takeIf { valueEnd > valueStart }
    }

    private fun labelFor(attributes: String): String {
        val (_, height) = resolution(attributes)
        if (height != null) return "${height}p"

        val bandwidth = numericValue(attributes, "BANDWIDTH")
        return when {
            bandwidth >= 4_500_000 -> "1080p"
            bandwidth >= 2_200_000 -> "720p"
            bandwidth >= 1_000_000 -> "480p"
            bandwidth > 0 -> "360p"
            else -> DownloadRequest.DEFAULT_QUALITY
        }
    }

    private fun resolution(attributes: String): Pair<Int?, Int?> {
        val start = attributes.indexOf("RESOLUTION=", ignoreCase = true)
        if (start < 0) return null to null
        val dimensions = attributes.substring(start + "RESOLUTION=".length)
            .split(',')[0].trim().split('x')
        val width = dimensions.getOrNull(0)?.toIntOrNull()
        val height = dimensions.getOrNull(1)?.toIntOrNull()
        return width to height
    }

    private fun numericValue(text: String, key: String): Int {
        val start = text.indexOf("$key=", ignoreCase = true)
        if (start < 0) return 0
        val valueStart = start + key.length + 1
        val valueEnd = text.indexOf(',', valueStart).let { if (it < 0) text.length else it }
        return text.substring(valueStart, valueEnd).trim().takeWhile(Char::isDigit).toIntOrNull() ?: 0
    }

    private fun absolute(value: String, baseUrl: String): String {
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val base = baseUrl.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid playlist URL")
        return base.resolve(value)?.toString() ?: throw IllegalArgumentException("Invalid HLS URL")
    }

    private fun trimLine(line: String): String = line.trim()

    private fun isMediaUrl(line: String): Boolean {
        return line.isNotEmpty() && !line.startsWith("#")
    }
}
