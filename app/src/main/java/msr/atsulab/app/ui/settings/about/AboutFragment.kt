package msr.atsulab.app.ui.settings.about

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import msr.atsulab.app.BuildConfig
import msr.atsulab.app.R
import msr.atsulab.app.databinding.FragmentAboutBinding
import msr.atsulab.app.helper.extensions.applyBottomSidePaddingInsets
import msr.atsulab.app.helper.extensions.applyTopPaddingInsets
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.ui.base.BaseFragment
import msr.atsulab.app.ui.base.NavigationManager
import org.koin.androidx.viewmodel.ext.android.viewModel


class AboutFragment : BaseFragment<FragmentAboutBinding, AboutViewModel>() {

    override val viewModel: AboutViewModel by viewModel()

    override fun generateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAboutBinding {
        return FragmentAboutBinding.inflate(inflater, container, false)
    }

    override fun setUpLayout() {
        binding.apply {
            setUpToolbar(defaultToolbar.defaultToolbar, getString(R.string.about_al_chan))

            aboutSettingsAppVersionText.text = getString(R.string.version, BuildConfig.VERSION_NAME)

            aboutSettingsAniListLink.clicks {
                navigation.openWebView(NavigationManager.Url.ALCHAN_FORUM_THREAD)
            }

            aboutSettingsGitHubLink.clicks {
                navigation.openWebView(NavigationManager.Url.ALCHAN_GITHUB)
            }

            aboutSettingsGmailLink.clicks {
                navigation.openEmailClient()
            }

            aboutSettingsPlayStoreLink.clicks {
                navigation.openWebView(NavigationManager.Url.ALCHAN_PLAY_STORE)
            }

            aboutSettingsTwitterLink.clicks {
                navigation.openWebView(NavigationManager.Url.ALCHAN_TWITTER)
            }

            aboutSettingsPrivacyPolicyText.clicks {
                navigation.openWebView(NavigationManager.Url.ALCHAN_PRIVACY_POLICY)
            }
        }
    }

    override fun setUpInsets() {
        binding.defaultToolbar.defaultToolbar.applyTopPaddingInsets()
        binding.aboutSettingsLayout.applyBottomSidePaddingInsets()
    }

    override fun setUpObserver() {
        // do nothing
    }

    companion object {
        @JvmStatic
        fun newInstance() = AboutFragment()
    }
}