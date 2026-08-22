package msr.atsulab.app.ui.landing

import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.R
import msr.atsulab.app.databinding.FragmentLandingBinding
import msr.atsulab.app.helper.extensions.applyTopBottomPaddingInsets
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.utils.ImageUtil
import msr.atsulab.app.ui.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel


class LandingFragment : BaseFragment<FragmentLandingBinding, LandingViewModel>() {

    override val viewModel: LandingViewModel by viewModel()

    override fun generateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLandingBinding {
        return FragmentLandingBinding.inflate(inflater, container, false)
    }

    override fun setUpLayout() {
        binding.apply {
            ImageUtil.loadImage(requireContext(), R.drawable.landing_wallpaper, landingBackgroundImage)

            landingGetStartedButton.clicks {
                navigation.navigateToLogin()
            }
        }
    }

    override fun setUpInsets() {
        binding.landingContentRoot.applyTopBottomPaddingInsets()
    }

    override fun setUpObserver() {
        // do nothing
    }

    companion object {
        @JvmStatic
        fun newInstance() = LandingFragment()
    }
}