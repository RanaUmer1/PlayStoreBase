package com.professor.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.R
import com.professor.pdfconverter.databinding.ActivityDocumentLoadBinding
import com.professor.pdfconverter.ui.viewmodels.ConversionViewModel
import com.professor.pdfconverter.utils.ApiResult
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import androidx.core.net.toUri

@AndroidEntryPoint
class DocumentLoadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentLoadBinding
    private val viewModel: ConversionViewModel by viewModels()

    private lateinit var filePath: String
    private lateinit var fileType: String
    private lateinit var fileName: String
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentLoadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setupUI()
        setupObservers()
        checkFileStatus()
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
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnConvert.setOnClickListener {
//            startConversion()
            startActivity(Intent(this, DownloadDocActivity::class.java))
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
                    showLoading(false)
                    state.data?.let { data ->
                        //handleConversionSuccess(data)
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

        // Create multipart file
        val filePart = try {
            val mediaType = when (fileType) {
                "pdf" -> "application/pdf".toMediaType()
                "doc", "docx" -> "application/msword".toMediaType()
                "excel", "xls", "xlsx" -> "application/vnd.ms-excel".toMediaType()
                "ppt", "pptx" -> "application/vnd.ms-powerpoint".toMediaType()
                else -> "*/*".toMediaType()
            }

            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(mediaType)
            )
        } catch (e: Exception) {
            showError("Failed to prepare file for upload: ${e.message}")
            return
        }

        // Start conversion
        viewModel.convertFile(filePart, conversionType)
    }

    private fun handleConversionSuccess(conversionData: Any) {
//        when (conversionData) {
//            is ConversionData -> {
//                showSuccessDialog(conversionData)
//            }
//            else -> {
//                showMessage("Conversion completed successfully")
//                // Navigate back or to download screen
//                onBackPressed()
//            }
//        }
    }

    /*
        private fun showSuccessDialog(conversionData: ConversionData) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Conversion Successful!")
                .setMessage(
                    """
                    Original: ${conversionData.originalFileName}
                    Converted: ${conversionData.convertedFileName}
                    Size: ${String.format("%.2f MB", conversionData.fileSize / (1024.0 * 1024.0))}

                    Download your file now?
                    """.trimIndent()
                )
                .setPositiveButton("Download") { _, _ ->
                    downloadConvertedFile(conversionData.downloadUrl)
                }
                .setNegativeButton("Later") { _, _ ->
                    onBackPressed()
                }
                .show()
        }
    */

    private fun downloadConvertedFile(downloadUrl: String) {
        viewModel.downloadFile(downloadUrl) { success, message ->
            if (success) {
                showMessage("File downloaded successfully")
                onBackPressed()
            } else {
                showError(message)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.btnConvert.isEnabled = false
            binding.btnConvert.text = getString(R.string.converting)
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
}