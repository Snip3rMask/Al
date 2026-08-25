package msr.atsulab.app.player.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.media3.ui.PlayerView
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import msr.atsulab.app.R
import msr.atsulab.app.player.engine.PlaybackReadyState
import msr.atsulab.app.player.engine.PlaybackState
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import java.io.File

internal class PlayerFrameCaptureManager(
    private val context: Context,
    private val playbackPreferencesStore: PlaybackPreferencesStore
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var saveDisposable: Disposable? = null
    private var isCapturing = false
    private var isReleased = false

    fun captureCurrentFrame(
        playerView: PlayerView,
        playbackState: PlaybackState,
        animeTitle: String,
        episodeLabel: String
    ) {
        if (!playbackPreferencesStore.isFrameCaptureEnabled()) return
        if (isCapturing) return
        if (
            playbackState.readyState != PlaybackReadyState.READY &&
            playbackState.readyState != PlaybackReadyState.ENDED
        ) {
            return
        }

        val surfaceView = playerView.videoSurfaceView ?: return showToast(R.string.player_capture_failed)
        if (surfaceView.width <= 0 || surfaceView.height <= 0) {
            return showToast(R.string.player_capture_failed)
        }

        isCapturing = true
        showToast(R.string.player_capture_saving)

        if (surfaceView is TextureView) {
            val bitmap = surfaceView.bitmap
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
                finishCapture(success = false, recycle = bitmap)
            } else {
                saveBitmap(bitmap, animeTitle, episodeLabel)
            }
            return
        }

        if (surfaceView is SurfaceView && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(
                surfaceView.width,
                surfaceView.height,
                Bitmap.Config.ARGB_8888
            )
            PixelCopy.request(
                surfaceView,
                bitmap,
                { result -> finishCapture(result == PixelCopy.SUCCESS, bitmap) },
                mainHandler
            )
            return
        }

        isCapturing = false
        showToast(R.string.player_capture_unsupported)
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        saveDisposable?.dispose()
        saveDisposable = null
        mainHandler.removeCallbacksAndMessages(null)
        isCapturing = false
    }

    private fun saveBitmap(
        bitmap: Bitmap,
        animeTitle: String,
        episodeLabel: String
    ) {
        saveDisposable?.dispose()
        saveDisposable = Single
            .fromCallable<File?> {
                val outputDirectory = outputDirectory(animeTitle)
                val outputFile = outputDirectory?.let { directory ->
                    File(directory, PlayerFrameCaptureNaming.fileName(episodeLabel))
                }
                if (outputFile != null) {
                    outputFile.outputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }
                outputFile
            }
            .subscribeOn(Schedulers.io())
            .doOnDispose { bitmap.recycle() }
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(
                { outputFile ->
                    if (outputFile == null) {
                        finishCapture(success = false, recycle = bitmap)
                    } else {
                        finishCapture(success = true, recycle = bitmap)
                        shareBitmap(outputFile)
                    }
                },
                {
                    finishCapture(success = false, recycle = bitmap)
                }
            )
    }

    private fun outputDirectory(animeTitle: String): File? {
        val picturesDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
        return File(picturesDirectory, "AtsuLab")
            .let { atsuLabDirectory -> File(atsuLabDirectory, PlayerFrameCaptureNaming.safeSegment(animeTitle)) }
            .apply { mkdirs() }
            .takeIf { it.isDirectory }
    }

    private fun shareBitmap(outputFile: File) {
        if (isReleased || context !is Activity || context.isFinishing) return
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.player.capture",
            outputFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, context.getString(R.string.player_capture_share))
        )
    }

    private fun finishCapture(success: Boolean, recycle: Bitmap?) {
        recycle?.recycle()
        isCapturing = false
        if (!isReleased) {
            showToast(
                if (success) R.string.player_capture_saved else R.string.player_capture_failed
            )
        }
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
    }
}

internal object PlayerFrameCaptureNaming {
    fun safeSegment(value: String): String {
        val cleaned = value.replace(ILLEGAL_FILE_CHARACTERS, "_").trim('_', ' ')
        return cleaned.ifBlank { DEFAULT_SEGMENT }.take(MAX_SEGMENT_LENGTH)
    }

    fun fileName(episodeLabel: String): String {
        return buildString {
            append("AtsuLab_")
            append(safeSegment(episodeLabel))
            append("_")
            append(System.currentTimeMillis())
            append(".png")
        }
    }

    private const val DEFAULT_SEGMENT = "frame"
    private const val MAX_SEGMENT_LENGTH = 60
    private val ILLEGAL_FILE_CHARACTERS = Regex("[^A-Za-z0-9-_ ]+")
}
