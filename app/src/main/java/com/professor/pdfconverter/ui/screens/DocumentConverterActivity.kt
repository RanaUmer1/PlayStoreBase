package com.professor.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.R
import com.professor.pdfconverter.data.source.api.ConversionResponse
import com.professor.pdfconverter.databinding.ActivityDocumentConverterBinding
import com.professor.pdfconverter.ui.viewmodels.ConversionViewModel
import com.professor.pdfconverter.utils.ApiResult
import com.professor.pdfconverter.utils.ProgressRequestBody
import com.professor.pdfconverter.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import java.io.File
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class DocumentConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentConverterBinding
    private val viewModel: ConversionViewModel by viewModels()

    private lateinit var filePath: String
    private lateinit var fileType: String
    private lateinit var fileName: String
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setupUI()
        setupObservers()
        checkFileStatus()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.loadingBar.visibility == View.VISIBLE) {
                    showExitDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun initData() {
        filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH) ?: ""
        fileType = intent.getStringExtra(Constants.EXTRA_FILE_TYPE) ?: ""
        fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME) ?: "document"
        val uriString = intent.getStringExtra(Constants.EXTRA_FILE_URI)
        fileUri = if (!uriString.isNullOrEmpty()) uriString.toUri() else null

        // Update UI with file info
        binding.tvDocumentTitle.text = fileName
        binding.tvFileName.text = fileName
        updateConvertButtonText()
    }

    private fun setupUI() {
        binding.btnBack.setClickWithTimeout {
            onBackPressed()
        }

        binding.btnConvert.setClickWithTimeout {
            startConversion()
        }

        // Initially hide convert button
        binding.btnConvert.visibility = View.GONE
        binding.loadingBar.visibility = View.VISIBLE

        if (fileType != "pdf") {
            binding.btnConvert.setBackgroundColor(resources.getColor(R.color.primary_color))
            binding.btnConvert.text = getString(R.string.convert_word_to_pdf)
            binding.ivFileIcon.setImageResource(R.drawable.ic_doc_small)
            binding.loadingBar.setIndicatorColor(resources.getColor(R.color.primary_color))
            binding.loadingBar.trackColor = resources.getColor(R.color.primary_color_14)
        } else {
            binding.btnConvert.setBackgroundColor(resources.getColor(R.color.red))
            binding.btnConvert.text = getString(R.string.convert_pdf_to_word)
            binding.ivFileIcon.setImageResource(R.drawable.ic_pdf_small)
            binding.loadingBar.setIndicatorColor(resources.getColor(R.color.red))
            binding.loadingBar.trackColor = resources.getColor(R.color.red_14)
        }

    }

    private fun setupObservers() {
        viewModel.conversionState.observe(this) { state ->
            when (state?.status) {
                ApiResult.Status.LOADING -> {
                    showLoading(true)
                }

                ApiResult.Status.SUCCESS -> {
                    state.data?.let { data ->
                        handleConversionSuccess(data)
                    }
                }

                ApiResult.Status.ERROR -> {
                    showLoading(false)
                    showError(state.message ?: "Conversion failed")
                }

                else -> Unit
            }
        }

        viewModel.fileStatus.observe(this) { isReady ->
            if (isReady) {
                // File is ready, show convert button
                binding.loadingBar.visibility = View.GONE
                binding.btnConvert.visibility = View.VISIBLE
            } else {
                // Still loading
                binding.loadingBar.visibility = View.VISIBLE
                binding.btnConvert.visibility = View.GONE
            }
        }

        viewModel.uploadProgress.observe(this) { progress ->
            // Only update during upload phase (when loading bar is visible and not downloading)
            if (binding.loadingBar.visibility == View.VISIBLE && 
                !binding.btnConvert.text.toString().startsWith("Downloading")) {
                binding.btnConvert.text = "Uploading $progress%"
            }
        }

        viewModel.downloadProgress.observe(this) { progress ->
            // Only update if we're in download phase
            if (binding.loadingBar.visibility == View.VISIBLE && 
                binding.btnConvert.text.toString().startsWith("Downloading")) {
                binding.btnConvert.text = "Downloading $progress%"
            }
        }
    }

    private fun checkFileStatus() {
        val file = File(filePath)
        if (file.exists() && file.length() > 0) {
            // File exists and has content
            viewModel.setFileReady(true)
        } else {
            // Try to load file from URI
            fileUri?.let { uri ->
                viewModel.loadFileFromUri(this, uri)
            } ?: run {
                showError("File not found")
            }
        }
    }

    private fun startConversion() {
        val file = File(filePath)
        if (!file.exists()) {
            showError("File not found. Please try again.")
            return
        }

        // Determine conversion type based on file type
        val conversionType = when (fileType) {
            "pdf" -> Constants.CONVERSION_PDF_TO_WORD
            "doc", "docx" -> Constants.CONVERSION_WORD_TO_PDF
            "excel", "xls", "xlsx" -> Constants.CONVERSION_EXCEL_TO_PDF
            "ppt", "pptx" -> Constants.CONVERSION_PPT_TO_PDF
            else -> Constants.CONVERSION_PDF_TO_WORD // default
        }

        // Create multipart file with progress tracking
        val filePart = try {
            val mediaType = when (fileType) {
                "pdf" -> "application/pdf".toMediaType()
                "doc", "docx" -> "application/msword".toMediaType()
                "excel", "xls", "xlsx" -> "application/vnd.ms-excel".toMediaType()
                "ppt", "pptx" -> "application/vnd.ms-powerpoint".toMediaType()
                else -> "*/*".toMediaType()
            }

            // Use ProgressRequestBody to track upload progress
            val progressRequestBody = ProgressRequestBody(file, mediaType) { progress ->
                viewModel.updateUploadProgress(progress)
            }

            MultipartBody.Part.createFormData(
                "file",
                file.name,
                progressRequestBody
            )
        } catch (e: Exception) {
            showError("Failed to prepare file for upload: ${e.message}")
            return
        }

        // Start conversion
        viewModel.convertFile(filePart, conversionType)
    }

    private fun handleConversionSuccess(conversionData: Any) {
        if (conversionData is ConversionResponse) {
            val fileList = conversionData.data.fileInfoDTOList
            if (fileList.isNotEmpty()) {
                val downloadUrl = fileList[0].downloadUrl
                if (downloadUrl.isNotEmpty()) {
                    // Update UI for downloading state
                    binding.btnConvert.text = "Downloading 0%"
                    binding.loadingBar.visibility = View.VISIBLE
                    binding.btnConvert.isEnabled = false

                    // Determine new extension
                    val newExtension = when (fileType) {
                        "pdf" -> "docx" // PDF -> Word
                        "doc", "docx" -> "pdf" // Word -> PDF
                        "excel", "xls", "xlsx" -> "pdf" // Excel -> PDF
                        "ppt", "pptx" -> "pdf" // PPT -> PDF
                        else -> "pdf"
                    }
                    
                    val nameWithoutExt = if (fileName.contains(".")) {
                        fileName.substringBeforeLast(".")
                    } else {
                        fileName
                    }
                    
                    val newFileName = "$nameWithoutExt.$newExtension"

                    downloadConvertedFile(downloadUrl, newFileName)
                } else {
                    showError("Download URL not found")
                }
            } else {
                showError("No file info returned")
            }
        } else {
            // Fallback or error handling if data type doesn't match
            showError("Invalid conversion response")
        }
    }

    private fun downloadConvertedFile(downloadUrl: String, fileName: String) {
        viewModel.downloadFile(downloadUrl, fileName) { success, message, filePath ->
            if (success) {
                showMessage("File downloaded successfully")
                // Navigate to SuccessActivity after successful download
                startActivity(Intent(this, SuccessActivity::class.java)
                    .putExtra(Constants.EXTRA_FILE_PATH, filePath)
                    .putExtra(Constants.EXTRA_FILE_NAME, File(filePath!!).name)
                )
                finish() // Optional: finish current activity so back button doesn't come back here
            } else {
                showLoading(false) // Hide loading bar and reset UI
                showError(message)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.btnConvert.isEnabled = false
            binding.btnConvert.text = "Uploading 0%"
            binding.loadingBar.visibility = View.VISIBLE
        } else {
            binding.btnConvert.isEnabled = true
            updateConvertButtonText()
            binding.loadingBar.visibility = View.GONE
        }
    }

    private fun updateConvertButtonText() {
        val buttonText = when (fileType) {
            "pdf" -> getString(R.string.convert_pdf_to_word)
            "doc", "docx" -> getString(R.string.convert_word_to_pdf)
            "excel", "xls", "xlsx" -> getString(R.string.convert_excel_to_pdf)
            "ppt", "pptx" -> getString(R.string.convert_ppt_to_pdf)
            else -> getString(R.string.convert_file)
        }
        binding.btnConvert.text = buttonText
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.btnConvert.isEnabled = true
        updateConvertButtonText()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetDownloadState()
    }

    private fun showExitDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_conversion_exit)
        dialog.window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val margin = (20 * displayMetrics.density).toInt()
        dialog.window?.setLayout(
            width - (2 * margin),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )


        dialog.findViewById<View>(R.id.btnExit).setClickWithTimeout {
            dialog.dismiss()
            finish()
        }

        dialog.findViewById<View>(R.id.btnWait).setClickWithTimeout {
            dialog.dismiss()
        }

        dialog.show()
    }
}
