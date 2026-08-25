package msr.atsulab.app.ui.settings.capture

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import msr.atsulab.app.R
import msr.atsulab.app.databinding.FragmentCaptureSettingsBinding
import msr.atsulab.app.helper.extensions.applyBottomPaddingInsets
import msr.atsulab.app.helper.extensions.applyTopPaddingInsets
import msr.atsulab.app.helper.extensions.clicks
import msr.atsulab.app.ui.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class CaptureSettingsFragment :
    BaseFragment<FragmentCaptureSettingsBinding, CaptureSettingsViewModel>() {

    override val viewModel: CaptureSettingsViewModel by viewModel()

    private var selectedFolderUri: Uri? = null

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let(::saveSelectedFolder)
    }

    override fun generateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCaptureSettingsBinding {
        return FragmentCaptureSettingsBinding.inflate(inflater, container, false)
    }

    override fun setUpLayout() {
        binding.apply {
            setUpToolbar(defaultToolbar.defaultToolbar, getString(R.string.capture_settings))

            captureEnabledCheckBox.setOnClickListener {
                viewModel.setEnabled(captureEnabledCheckBox.isChecked)
            }
            captureEnabledLayout.clicks {
                captureEnabledCheckBox.toggle()
            }

            captureAlwaysVisibleCheckBox.setOnClickListener {
                viewModel.setAlwaysVisible(captureAlwaysVisibleCheckBox.isChecked)
            }
            captureAlwaysVisibleLayout.clicks {
                captureAlwaysVisibleCheckBox.toggle()
            }

            captureChooseFolderButton.clicks {
                openFolderPicker()
            }
            captureSaveLocationLayout.clicks {
                openFolderPicker()
            }
            captureResetPositionLayout.clicks {
                viewModel.resetButtonPosition()
                dialog.showToast(R.string.capture_button_position_reset)
            }
        }
    }

    override fun setUpInsets() {
        binding.defaultToolbar.defaultToolbar.applyTopPaddingInsets()
        binding.captureScroll.applyBottomPaddingInsets()
    }

    override fun setUpObserver() {
        disposables.addAll(
            viewModel.isFrameCaptureEnabled.subscribe {
                binding.captureEnabledCheckBox.isChecked = it
            },
            viewModel.isFrameCaptureAlwaysVisible.subscribe {
                binding.captureAlwaysVisibleCheckBox.isChecked = it
            },
            viewModel.frameCaptureDirectoryUri.subscribe { value ->
                val uri = value.takeIf { it.isNotBlank() }?.let(Uri::parse)
                selectedFolderUri = uri
                binding.captureLocationValue.text = uri?.let(::folderDisplayName)
                    ?: getString(R.string.default_app_folder)
            }
        )
    }

    private fun openFolderPicker() {
        runCatching { folderPicker.launch(null) }
    }

    private fun saveSelectedFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            requireContext().contentResolver.takePersistableUriPermission(uri, flags)
            releasePreviousFolder(uri)
            viewModel.setDirectory(uri.toString())
        } catch (_: SecurityException) {
            dialog.showToast(R.string.capture_save_folder_failed)
        }
    }

    private fun releasePreviousFolder(newUri: Uri) {
        val previousUri = selectedFolderUri ?: return
        if (previousUri == newUri) return
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            requireContext().contentResolver.releasePersistableUriPermission(previousUri, flags)
        }
    }

    private fun folderDisplayName(uri: Uri): String? {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
        return runCatching {
            requireContext().contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0) ?: documentId.substringAfter(':', missingDelimiterValue = documentId)
                } else {
                    documentId.substringAfter(':', missingDelimiterValue = documentId)
                }
            }
        }.getOrNull() ?: documentId.substringAfter(':', missingDelimiterValue = documentId)
    }

    companion object {
        @JvmStatic
        fun newInstance() = CaptureSettingsFragment()
    }
}
