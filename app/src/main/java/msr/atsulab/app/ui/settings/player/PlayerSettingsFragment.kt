package msr.atsulab.app.ui.settings.player

import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import msr.atsulab.app.R
import msr.atsulab.app.databinding.FragmentPlayerSettingsBinding
import msr.atsulab.app.helper.extensions.applyBottomPaddingInsets
import msr.atsulab.app.helper.extensions.applyTopPaddingInsets
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.ui.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlayerSettingsFragment :
    BaseFragment<FragmentPlayerSettingsBinding, PlayerSettingsViewModel>() {

    override val viewModel: PlayerSettingsViewModel by viewModel()

    override fun generateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPlayerSettingsBinding {
        return FragmentPlayerSettingsBinding.inflate(inflater, container, false)
    }

    override fun setUpLayout() {
        binding.apply {
            setUpToolbar(defaultToolbar.defaultToolbar, getString(R.string.player_settings))

            autoRotateCheckBox.setOnClickListener {
                viewModel.setAutoRotate(autoRotateCheckBox.isChecked)
            }
            autoRotateLayout.clicks {
                autoRotateCheckBox.toggle()
                viewModel.setAutoRotate(autoRotateCheckBox.isChecked)
            }

            rememberLastEpisodeCheckBox.setOnClickListener {
                viewModel.setRememberLastEpisode(rememberLastEpisodeCheckBox.isChecked)
            }
            rememberLastEpisodeLayout.clicks {
                rememberLastEpisodeCheckBox.toggle()
                viewModel.setRememberLastEpisode(rememberLastEpisodeCheckBox.isChecked)
            }

            seekDurationLayout.clicks { showSeekDurationPicker() }
        }
    }

    override fun setUpInsets() {
        binding.defaultToolbar.defaultToolbar.applyTopPaddingInsets()
        binding.playerSettingsScroll.applyBottomPaddingInsets()
    }

    override fun setUpObserver() {
        disposables.addAll(
            viewModel.isAutoRotate.subscribe {
                binding.autoRotateCheckBox.isChecked = it
            },
            viewModel.isRememberLastEpisode.subscribe {
                binding.rememberLastEpisodeCheckBox.isChecked = it
            },
            viewModel.seekDurationSeconds.subscribe { seconds ->
                binding.seekDurationValue.text = getString(R.string.seek_seconds_format, seconds)
            }
        )
    }

    private fun showSeekDurationPicker() {
        val options = intArrayOf(5, 10, 15, 20, 30)
        val labels = options.map { getString(R.string.seek_seconds_format, it) }.toTypedArray()
        val current = viewModel.seekDurationSeconds.blockingFirst()
        val checkedIndex = options.indexOf(current).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.skip_duration)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                viewModel.setSeekDuration(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = PlayerSettingsFragment()
    }
}
