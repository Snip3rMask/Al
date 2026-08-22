package msr.atsulab.app.ui.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import msr.atsulab.app.R
import msr.atsulab.app.databinding.DialogBottomSheetProgressBinding
import msr.atsulab.app.helper.enums.MediaType
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.helper.extensions.show
import msr.atsulab.app.ui.base.BaseDialogFragment

class BottomSheetProgressDialog : BaseDialogFragment<DialogBottomSheetProgressBinding>() {

    private var listener: BottomSheetProgressListener? = null
    private var mediaType = MediaType.ANIME
    private var currentProgress = 0
    private var maxProgress: Int? = null
    private var isProgressVolume = false

    override fun generateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogBottomSheetProgressBinding {
        return DialogBottomSheetProgressBinding.inflate(inflater, container, false)
    }

    override fun setUpLayout() {
        binding.apply {
            progressCurrentProgressText.text = currentProgress.toString()
            progressMaxProgressText.text = "/ ${maxProgress ?: "?"}"

            progressIncrementButton.text = if (isProgressVolume) {
                getString(R.string.plus_one_vo)
            } else {
                when (mediaType) {
                    MediaType.ANIME -> getString(R.string.plus_one_ep)
                    MediaType.MANGA -> getString(R.string.plus_one_ch)
                }
            }

            progressIncrementButton.show(currentProgress < (maxProgress ?: Int.MAX_VALUE))

            progressIncrementButton.clicks {
                listener?.getNewProgress(currentProgress + 1)
                dismiss()
            }

            progressSetButton.clicks {
                var newProgress = progressNextProgressEditText.text.toString().toIntOrNull() ?: 0
                maxProgress?.let {
                    if (newProgress > it) newProgress = it
                }

                listener?.getNewProgress(newProgress)
                dismiss()
            }

            progressNextProgressEditText.requestFocus()
            openKeyboard()
        }
    }

    override fun setUpObserver() {
        // do nothing
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener = null
    }

    companion object {
        fun newInstance(
            mediaType: MediaType,
            currentProgress: Int,
            maxProgress: Int? = null,
            isProgressVolume: Boolean = false,
            listener: BottomSheetProgressListener
        ) = BottomSheetProgressDialog().apply {
            this.mediaType = mediaType
            this.currentProgress = currentProgress
            this.maxProgress = maxProgress
            this.isProgressVolume = isProgressVolume
            this.listener = listener
        }
    }

    interface BottomSheetProgressListener {
        fun getNewProgress(newProgress: Int)
    }
}