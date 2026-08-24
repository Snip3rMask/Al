package msr.atsulab.app.player.ui

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

internal class PlayerGestureHandler(
    context: Context,
    private val callbacks: Callbacks
) : View.OnTouchListener {

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                callbacks.onSingleTap()
                return true
            }

            override fun onDoubleTap(event: MotionEvent): Boolean {
                val half = context.resources.displayMetrics.widthPixels / 2f
                callbacks.onSeek(event.x < half)
                return true
            }
        }
    )

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (callbacks.isControlsLocked()) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                callbacks.onLockedTouch()
            }
            return true
        }
        return detector.onTouchEvent(event)
    }

    internal interface Callbacks {
        fun isControlsLocked(): Boolean
        fun onLockedTouch()
        fun onSingleTap()
        fun onSeek(isForward: Boolean)
    }
}
