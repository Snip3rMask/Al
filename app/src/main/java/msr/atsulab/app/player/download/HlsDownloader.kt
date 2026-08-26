package msr.atsulab.app.player.download

import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.OkHttpClient
import okhttp3.Request

class HlsDownloader(
    private val httpClient: OkHttpClient,
    private val outputDirectory: File,
    private val maxParallelSegments: Int = DEFAULT_PARALLEL_SEGMENTS
) {

    fun download(
        request: DownloadRequest,
        parallelSegments: Int = DEFAULT_PARALLEL_SEGMENTS.coerceAtMost(maxParallelSegments),
        cancelToken: DownloadCancelToken = DownloadCancelToken.NEVER,
        onProgress: DownloadProgressListener = DownloadProgressListener { _, _ -> }
    ): File {
        check(!cancelToken.isCancelled()) { "Download cancelled" }
        outputDirectory.mkdirs()
        require(outputDirectory.isDirectory) { "Cannot create download directory" }

        var playlistUrl = request.url
        val masterContent = getText(playlistUrl, request.referer, cancelToken)
        if (HlsPlaylistParser.isMasterPlaylist(masterContent)) {
            playlistUrl = HlsPlaylistParser.parseMaster(masterContent, playlistUrl)
                .maxByOrNull { it.bandwidth }?.url
                ?: throw IOException("No playable HLS variant found")
        }

        val mediaContent = getText(playlistUrl, request.referer, cancelToken)
        val playlist = HlsPlaylistParser.parseMedia(mediaContent, playlistUrl)
        if (playlist.isEmpty) throw IOException("No HLS segments found")

        val temporaryDirectory = File(outputDirectory, ".tmp-${System.nanoTime()}")
        check(temporaryDirectory.mkdirs()) { "Cannot create temporary download directory" }

        try {
            downloadSegments(playlist.segmentUrls, temporaryDirectory, parallelSegments, cancelToken, onProgress)
            val outputFile = File(outputDirectory, request.fileName)
            val partialFile = File(outputDirectory, request.fileName + PARTIAL_SUFFIX)
            merge(playlist.initializationUrl, temporaryDirectory, playlist.segmentUrls.size, partialFile, request.referer, cancelToken)

            if (outputFile.exists() && !outputFile.delete()) {
                throw IOException("Could not replace existing download")
            }
            if (!partialFile.renameTo(outputFile)) {
                throw IOException("Could not finalize downloaded episode")
            }
            return outputFile
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun downloadSegments(
        segmentUrls: List<String>,
        temporaryDirectory: File,
        parallelSegments: Int,
        cancelToken: DownloadCancelToken,
        onProgress: DownloadProgressListener
    ) {
        val executor = newExecutor(parallelSegments)
        val completed = AtomicInteger(0)
        val futures = segmentUrls.mapIndexed { index, segmentUrl ->
            executor.submit {
                val target = File(temporaryDirectory, String.format(Locale.US, "%06d.seg", index))
                downloadWithRetry(segmentUrl, target, cancelToken)
                onProgress.onProgress(completed.incrementAndGet(), segmentUrls.size)
            }
        }

        try {
            futures.forEach { future -> future.get() }
        } catch (exception: Exception) {
            executor.shutdownNow()
            throw unwrapDownloadException(exception)
        } finally {
            executor.shutdown()
        }
    }

    private fun newExecutor(parallelSegments: Int): ExecutorService {
        val workerCount = parallelSegments.coerceIn(MIN_PARALLEL_SEGMENTS, maxParallelSegments)
        return Executors.newFixedThreadPool(workerCount)
    }

    private fun downloadWithRetry(url: String, target: File, cancelToken: DownloadCancelToken) {
        var lastError: IOException? = null
        repeat(MAX_SEGMENT_ATTEMPTS) { attempt ->
            try {
                check(!cancelToken.isCancelled()) { "Download cancelled" }
                copyToFile(url, target)
                return
            } catch (exception: IOException) {
                lastError = exception
                if (attempt < MAX_SEGMENT_ATTEMPTS - 1) {
                    try {
                        Thread.sleep((RETRY_BACKOFF_MS * (attempt + 1)))
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Download interrupted", interrupted)
                    }
                }
            }
        }
        throw lastError ?: IOException("Segment download failed")
    }

    private fun merge(
        initializationUrl: String?,
        temporaryDirectory: File,
        segmentCount: Int,
        target: File,
        referer: String,
        cancelToken: DownloadCancelToken
    ) {
        target.outputStream().use { output ->
            initializationUrl?.let { url -> appendRemote(url, referer, cancelToken, output) }
            repeat(segmentCount) { index ->
                check(!cancelToken.isCancelled()) { "Download cancelled" }
                val segment = File(temporaryDirectory, String.format(Locale.US, "%06d.seg", index))
                segment.inputStream().use { input -> input.copyTo(output) }
            }
        }
    }

    private fun getText(url: String, referer: String, cancelToken: DownloadCancelToken): String {
        check(!cancelToken.isCancelled()) { "Download cancelled" }
        return execute(newRequest(url, referer), cancelToken) { response -> response.body?.string() }
            ?: throw IOException("Empty HLS playlist response")
    }

    private fun appendRemote(url: String, referer: String, cancelToken: DownloadCancelToken, output: java.io.OutputStream) {
        execute(newRequest(url, referer), cancelToken) { response ->
            response.body?.byteStream()?.use { input -> input.copyTo(output) }
            true
        } ?: throw IOException("HLS initialization download failed")
    }

    private fun copyToFile(url: String, target: File) {
        execute(newRequest(url, "")) { response ->
            response.body?.byteStream()?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Empty segment response")
            true
        } ?: throw IOException("Segment download failed")
    }

    private inline fun <T> execute(request: Request, cancelToken: DownloadCancelToken = DownloadCancelToken.NEVER, read: (okhttp3.Response) -> T): T? {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HLS request failed with HTTP ${response.code}")
            check(!cancelToken.isCancelled()) { "Download cancelled" }
            return read(response)
        }
    }

    private fun newRequest(url: String, referer: String): Request {
        return Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .apply {
                if (referer.isNotBlank()) header("Referer", referer)
            }
            .build()
    }

    private fun unwrapDownloadException(exception: Exception): Exception {
        val cause = exception.cause
        return cause as? Exception ?: IOException("Parallel HLS download failed", exception)
    }

    companion object {
        const val DEFAULT_PARALLEL_SEGMENTS = 16
        const val MIN_PARALLEL_SEGMENTS = 1
        private const val MAX_SEGMENT_ATTEMPTS = 3
        private const val RETRY_BACKOFF_MS = 500L
        private const val PARTIAL_SUFFIX = ".part"
        private const val USER_AGENT = "Mozilla/5.0"
    }
}
