package msr.atsulab.app.player.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.SurfaceView
import android.view.TextureView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.annotation.RequiresApi
import androidx.media3.ui.PlayerView
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import msr.atsulab.app.R
import msr.atsulab.app.player.engine.PlaybackReadyState
import msr.atsulab.app.player.engine.PlaybackState
import msr.atsulab.app.player.storage.PlaybackPreferencesStore
import java.io.File
import java.io.IOException

internal class PlayerFrameCaptureManager(
    private val context: Context,
    private val playbackPreferencesStore: PlaybackPreferencesStore
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var saveDisposable: Disposable? = null
    private var isCapturing = false
    private var isReleased = false
    private var isStarted = false
    private var dragStartRawX = 0f
    private var dragStartRawY = 0f
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false

    fun start() {
        isStarted = true
    }

    fun stop() {
        isStarted = false
    }

    fun attachCaptureButton(
        host: FrameLayout,
        button: View,
        onCaptureClicked: () -> Unit
    ) {
        button.setOnClickListener { onCaptureClicked() }
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartRawX = event.rawX
                    dragStartRawY = event.rawY
                    dragStartX = view.x
                    dragStartY = view.y
                    isDragging = false
                    host.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - dragStartRawX
                    val deltaY = event.rawY - dragStartRawY
                    if (abs(deltaX) > 6f || abs(deltaY) > 6f) {
                        isDragging = true
                        moveControl(host, view, dragStartX + deltaX, dragStartY + deltaY)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    host.requestDisallowInterceptTouchEvent(false)
                    if (isDragging) {
                        saveControlPosition(host, view)
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        view.performClick()
                    }
                    true
                }
                else -> true
            }
        }
        host.post { positionCaptureButton(host, button) }
    }

    private fun positionCaptureButton(host: FrameLayout, button: View) {
        val savedX = playbackPreferencesStore.getFrameCapturePositionX()
        val savedY = playbackPreferencesStore.getFrameCapturePositionY()
        if (savedX >= 0f && savedY >= 0f) {
            moveControl(
                host,
                button,
                savedX * maxOf(0f, (host.width - button.width).toFloat()),
                savedY * maxOf(0f, (host.height - button.height).toFloat())
            )
            return
        }

        val density = host.resources.displayMetrics.density
        moveControl(
            host,
            button,
            host.width - button.width - 24.dpToPixels(density),
            86.dpToPixels(density)
        )
    }

    private fun moveControl(host: FrameLayout, button: View, targetX: Float, targetY: Float) {
        button.x = targetX.coerceIn(0f, maxOf(0f, (host.width - button.width).toFloat()))
        button.y = targetY.coerceIn(0f, maxOf(0f, (host.height - button.height).toFloat()))
    }

    private fun saveControlPosition(host: FrameLayout, button: View) {
        val maxX = maxOf(1f, (host.width - button.width).toFloat())
        val maxY = maxOf(1f, (host.height - button.height).toFloat())
        playbackPreferencesStore.setFrameCapturePosition(button.x / maxX, button.y / maxY)
    }

    private fun Int.dpToPixels(density: Float): Float = this * density

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

        if (isReleased) return
        val surfaceView = playerView.videoSurfaceView
            ?: return showToast(R.string.player_capture_failed)
        if (surfaceView.width <= 0 || surfaceView.height <= 0) {
            return showToast(R.string.player_capture_failed)
        }

        if (surfaceView !is TextureView && surfaceView !is SurfaceView) {
            return showToast(R.string.player_capture_unsupported)
        }
        if (surfaceView is SurfaceView && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return showToast(R.string.player_capture_unsupported)
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

        if (surfaceView is SurfaceView) {
            captureSurfaceView(surfaceView, animeTitle, episodeLabel)
            return
        }
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        isCapturing = false
        isStarted = false
    }

    private fun saveBitmap(
        bitmap: Bitmap,
        animeTitle: String,
        episodeLabel: String
    ) {
        saveDisposable?.dispose()
        saveDisposable = Maybe
            .fromCallable {
                val outputDirectory = outputDirectory(animeTitle)
                val directory = outputDirectory
                    ?: throw IOException("Frame capture pictures directory is unavailable")
                val outputFile = File(
                    directory,
                    PlayerFrameCaptureNaming.fileName(episodeLabel)
                )
                try {
                    outputFile.outputStream().use { stream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                            throw IOException("Frame bitmap compression failed")
                        }
                    }
                } catch (error: Throwable) {
                    outputFile.delete()
                    throw error
                }
                outputFile
            }
            .subscribeOn(Schedulers.io())
            .doOnDispose { bitmap.recycle() }
            .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(
                { outputFile ->
                    finishCapture(success = true, recycle = bitmap)
                    shareBitmap(outputFile)
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
        if (isReleased || !isStarted || context !is Activity) return
        val activity = context as Activity
        if (activity.isFinishing || activity.isDestroyed) return
        val contentUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.player.capture",
            outputFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newRawUri("frame", contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            activity.startActivity(
                Intent.createChooser(shareIntent, activity.getString(R.string.player_capture_share))
            )
        } catch (_: ActivityNotFoundException) {
        }
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureSurfaceView(
        surfaceView: SurfaceView,
        animeTitle: String,
        episodeLabel: String
    ) {
        val directBitmap = Bitmap.createBitmap(
            surfaceView.width,
            surfaceView.height,
            Bitmap.Config.ARGB_8888
        )
        PixelCopy.request(
            surfaceView,
            directBitmap,
            { result ->
                when {
                    result == PixelCopy.SUCCESS -> saveBitmap(directBitmap, animeTitle, episodeLabel)
                    isReleased -> directBitmap.recycle()
                    else -> {
                        directBitmap.recycle()
                        captureWindow(surfaceView, animeTitle, episodeLabel)
                    }
                }
            },
            mainHandler
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun captureWindow(
        surfaceView: SurfaceView,
        animeTitle: String,
        episodeLabel: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return finishCapture(success = false, recycle = null)
        }
        val activity = context as? Activity ?: return finishCapture(false, null)
        val window = activity.window ?: return finishCapture(false, null)
        val location = IntArray(2)
        surfaceView.getLocationInWindow(location)
        val contentRect = Rect(
            location[0],
            location[1],
            location[0] + surfaceView.width,
            location[1] + surfaceView.height
        )
        val visibleRect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(visibleRect)
        contentRect.intersect(visibleRect)
        if (contentRect.width() <= 0 || contentRect.height() <= 0) {
            return finishCapture(success = false, recycle = null)
        }

        val windowBitmap = Bitmap.createBitmap(
            contentRect.width(),
            contentRect.height(),
            Bitmap.Config.ARGB_8888
        )
        PixelCopy.request(
            window,
            contentRect,
            windowBitmap,
            { result -> finishCapture(result == PixelCopy.SUCCESS, windowBitmap) },
            mainHandler
        )
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
