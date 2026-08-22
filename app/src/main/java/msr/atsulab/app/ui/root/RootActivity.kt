package msr.atsulab.app.ui.root

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import msr.atsulab.app.databinding.ActivityRootBinding
import msr.atsulab.app.helper.utils.DeepLink
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.helper.utils.PushNotificationUtil
import msr.atsulab.app.type.NotificationUnion
import msr.atsulab.app.ui.base.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class RootActivity : BaseActivity<ActivityRootBinding>() {

    override lateinit var dialogManager: DialogManager

    override lateinit var navigationManager: NavigationManager

    private val _incomingDeepLink = PublishSubject.create<DeepLink>()
    override val incomingDeepLink: Observable<DeepLink>
        get() = _incomingDeepLink

    private var newIntent: Intent? = null

    override fun generateViewBinding(): ActivityRootBinding {
        return ActivityRootBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navigationManager.shouldPopFromBrowseScreen()) {
                    navigationManager.popBrowseScreenPage()
                } else if (navigationManager.hasBackStack()) {
                    navigationManager.popBackStack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun setUpLayout() {
        navigationManager = DefaultNavigationManager(this, supportFragmentManager, binding.rootLayout)
        dialogManager = DefaultDialogManager(this)

        ImageUtil.init(this)

        handleDeepLink(intent)
        requestNotificationPermission()
    }

    override fun setUpObserver() {
        // do nothing
    }

    override fun onResume() {
        super.onResume()
        newIntent?.let {
            handleDeepLink(it)
            newIntent = null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.data != null) {
            newIntent = intent
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val deepLink = DeepLink(intent.data)

        when {
            supportFragmentManager.fragments.isEmpty() -> {
                navigationManager.navigateToSplash(deepLink, intent.getBooleanExtra("RESTART", false))
            }
            navigationManager.isAtPreLoginScreen() -> {
                if (deepLink.isLogin()) {
                    val uri = deepLink.uri
                    val code = uri?.getQueryParameter("code")
                    if (code != null) {
                        exchangeCodeForToken(code)
                    } else {
                        val fullDeepLink = uri?.encodedFragment
                        val accessToken = fullDeepLink?.substring("access_token=".length, fullDeepLink.indexOf("&"))
                        navigationManager.navigateToLogin(accessToken, true)
                    }
                } else {
                    navigationManager.navigateToMain(deepLink)
                }
                intent.data = null
            }
            deepLink.uri != null -> {
                _incomingDeepLink.onNext(deepLink)
                intent.data = null
            }
        }
    }

    private fun exchangeCodeForToken(code: String) {
        Thread {
            try {
                val client = okhttp3.OkHttpClient()
                val form = okhttp3.FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("client_id", msr.atsulab.app.helper.Constant.ANILIST_CLIENT_ID.toString())
                    .add("client_secret", msr.atsulab.app.helper.Constant.ANILIST_CLIENT_SECRET)
                    .add("redirect_uri", applicationContext.packageName + "://anilist")
                    .add("code", code)
                    .build()
                val req = okhttp3.Request.Builder().url(msr.atsulab.app.helper.Constant.ANILIST_TOKEN_URL).post(form).build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string() ?: ""
                    val token = org.json.JSONObject(body).optString("access_token")
                    if (token.isNotEmpty()) {
                        runOnUiThread { navigationManager.navigateToLogin(token, true) }
                    } else {
                        runOnUiThread { android.widget.Toast.makeText(this, "Login failed: " + body.take(200), android.widget.Toast.LENGTH_LONG).show() }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { android.widget.Toast.makeText(this, "Login error: " + (e.message ?: ""), android.widget.Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun requestNotificationPermission() {
        PushNotificationUtil.createNotificationChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            val isPermissionDenied = checkSelfPermission(notificationPermission) != PackageManager.PERMISSION_GRANTED
            val isNeverDeniedOrCurrentlyGranted = !shouldShowRequestPermissionRationale(notificationPermission)
            if (isPermissionDenied && isNeverDeniedOrCurrentlyGranted) {
                requestPermissions(arrayOf(notificationPermission), 0)
            }
        }
    }
}