package msr.atsulab.app.ui.base

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding
import msr.atsulab.app.R
import msr.atsulab.app.helper.crash.CrashReporter
import msr.atsulab.app.helper.utils.DeepLink
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.hypot

abstract class BaseActivity<T: ViewBinding> : AppCompatActivity(), ViewContract {

    private val viewModel by viewModel<BaseActivityViewModel>()

    abstract var navigationManager: NavigationManager
        protected set

    abstract var dialogManager: DialogManager
        protected set

    abstract val incomingDeepLink: Observable<DeepLink>

    protected val disposables = CompositeDisposable()

    private var _binding: T? = null
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownTime = 0L
    private var touchDownDescription = ""
    protected val binding: T
        get() = _binding!!

    abstract fun generateViewBinding(): T

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashReporter.event("activity_create", javaClass.simpleName)
        super.onCreate(savedInstanceState)

        val appThemeResource = viewModel.getAppThemeResource()
        val isLightMode = viewModel.isLightMode()

        setTheme(appThemeResource)

        AppCompatDelegate.setDefaultNightMode(
            if (isLightMode) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        )

        _binding = generateViewBinding()
        CrashReporter.setCurrentScreen(javaClass.simpleName.ifBlank { "UnknownActivity" })
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> {
                controller.isAppearanceLightStatusBars = isLightMode
                window.navigationBarColor = getColor(R.color.pureBlack)
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                controller.isAppearanceLightStatusBars = isLightMode
                controller.isAppearanceLightNavigationBars = isLightMode
                window.navigationBarColor = getColor(if (isLightMode) R.color.whiteTransparent70 else R.color.pureBlackTransparent70)
            }
            else -> {
                controller.isAppearanceLightStatusBars = isLightMode
                controller.isAppearanceLightNavigationBars = isLightMode
            }
        }

        setUpLayout()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> runCatching {
                touchDownX = event.rawX
                touchDownY = event.rawY
                touchDownTime = System.currentTimeMillis()
                touchDownDescription = window?.decorView?.let { decorView ->
                    describeTouchTarget(findTouchTarget(decorView, event.rawX, event.rawY))
                }.orEmpty()
            }
            MotionEvent.ACTION_UP -> runCatching {
                val distance = hypot(event.rawX - touchDownX, event.rawY - touchDownY)
                val isQuickTap = System.currentTimeMillis() - touchDownTime <
                    ViewConfiguration.getLongPressTimeout() && distance <= 32f
                if (isQuickTap && touchDownDescription.isNotEmpty()) {
                    CrashReporter.event("tap", touchDownDescription)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun findTouchTarget(view: View, rawX: Float, rawY: Float): View? {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val containsTouch = rawX >= location[0] && rawX < location[0] + view.width &&
            rawY >= location[1] && rawY < location[1] + view.height
        if (view !is ViewGroup) {
            return if (containsTouch && view.visibility == View.VISIBLE) view else null
        }

        for (index in view.childCount - 1 downTo 0) {
            val child = view.getChildAt(index)
            val target = findTouchTarget(child, rawX, rawY)
            if (target != null) return target
        }
        return if (containsTouch && view.visibility == View.VISIBLE) view else null
    }

    private fun describeTouchTarget(view: View?): String {
        if (view == null) return "none"
        val className = view.javaClass.simpleName.ifBlank { view.javaClass.name.substringAfterLast('.') }
        val idName = runCatching { resources.getResourceEntryName(view.id) }.getOrDefault("no_id")
        val label = view.contentDescription?.toString()?.take(100)
            ?: (view as? android.widget.TextView)?.text?.toString()?.take(100)
        return buildString {
            append(className)
            append("/")
            append(idName)
            if (!label.isNullOrBlank()) {
                append(": ")
                append(label)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        CrashReporter.event("activity_start", javaClass.simpleName)
        setUpObserver()
    }

    override fun onResume() {
        super.onResume()
        CrashReporter.setCurrentScreen(javaClass.simpleName.ifBlank { "UnknownActivity" })
        CrashReporter.event("activity_resume", javaClass.simpleName)
        if (disposables.isDisposed) {
            setUpObserver()
        }
    }

    override fun onPause() {
        CrashReporter.event("activity_pause", javaClass.simpleName)
        super.onPause()
        disposables.clear()
    }

    override fun onStop() {
        CrashReporter.event("activity_stop", javaClass.simpleName)
        super.onStop()
        disposables.clear()
    }

    override fun onDestroy() {
        CrashReporter.event("activity_destroy", javaClass.simpleName)
        super.onDestroy()
        disposables.clear()
    }
}