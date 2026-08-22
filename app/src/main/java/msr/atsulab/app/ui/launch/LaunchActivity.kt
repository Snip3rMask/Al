package msr.atsulab.app.ui.launch

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import msr.atsulab.app.R
import msr.atsulab.app.databinding.ActivityLaunchBinding
import msr.atsulab.app.helper.utils.DeepLink
import msr.atsulab.app.ui.base.BaseActivity
import msr.atsulab.app.ui.base.DialogManager
import msr.atsulab.app.ui.base.NavigationManager
import msr.atsulab.app.ui.root.RootActivity
import io.reactivex.rxjava3.core.Observable

class LaunchActivity : BaseActivity<ActivityLaunchBinding>() {

    override lateinit var navigationManager: NavigationManager

    override lateinit var dialogManager: DialogManager

    override val incomingDeepLink: Observable<DeepLink>
        get() = Observable.never()

    override fun generateViewBinding(): ActivityLaunchBinding {
        return ActivityLaunchBinding.inflate(layoutInflater)
    }

    override fun setUpLayout() {
        val targetIntent = Intent(this, RootActivity::class.java)
        targetIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        targetIntent.data = intent.data
        targetIntent.putExtra("RESTART", intent.getBooleanExtra("RESTART", false))
        startActivity(targetIntent)
        overridePendingTransition(0, 0)
        finish()
    }

    override fun setUpObserver() {
        // do nothing
    }
}