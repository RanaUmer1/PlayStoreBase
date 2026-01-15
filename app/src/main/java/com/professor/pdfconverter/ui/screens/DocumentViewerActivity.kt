package com.professor.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.widget.DocView
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.databinding.ActivityDocumentViewerBinding
import com.professor.pdfconverter.utils.GetPath
import com.professor.pdfconverter.utils.Utils
import com.professor.pdfconverter.utils.setClickWithTimeout
import com.rajat.pdfviewer.PdfRendererView
import java.io.File

class DocumentViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentViewerBinding
    private var pdfView: PdfRendererView? = null
    private var docView: DocView? = null
    private var currentFile: File? = null
    private var fileType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        loadDocument()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.ivBack.setClickWithTimeout {
            onBackPressed()
        }

        binding.ivTick.setClickWithTimeout {
            openDocumentLoadActivity()
        }

        binding.ivShare.setClickWithTimeout {
            Utils.shareFile(currentFile?.name ?: "", currentFile?.absolutePath ?: "", this)
        }
    }


    private fun openDocumentLoadActivity() {
        currentFile?.let { file ->
            val intent = Intent(this, DocumentConverterActivity::class.java).apply {
                putExtra(Constants.EXTRA_FILE_PATH, file.absolutePath)
                putExtra(Constants.EXTRA_FILE_TYPE, fileType)
                putExtra(Constants.EXTRA_FILE_NAME, file.name)
                putExtra(Constants.EXTRA_FILE_URI, currentFileUri?.toString())
            }
            startActivity(intent)
        } ?: run {
            showMessage("Please wait for file to load completely")
        }
    }

    private fun loadDocument() {
        val uriString = intent.getStringExtra(Constants.EXTRA_FILE_URI)
        val title = intent.getStringExtra(Constants.EXTRA_FILE_NAME) ?: "Document"
        val adapterFile = intent.getBooleanExtra(Constants.EXTRA_FILE_VIEW_FROM_ADAPTER, false)

        binding.ivTick.isVisible = !adapterFile
        binding.ivShare.isVisible = adapterFile

        if (uriString.isNullOrEmpty()) {
            showError("No file provided")
            return
        }

        val uri = uriString.toUri()
        currentFileUri = uri
        binding.tvTitle.text = title

        // Determine file type and load appropriate viewer
        when {
            isPdfFile(title) -> {
                fileType = "pdf"
                initializePdfView(uri, adapterFile)
            }

            isDocFile(title) -> {
                Log.e("TAF", "loadDocument: $uri ", )
                Log.e("TAF2", "loadDocument: $currentFileUri ", )
                fileType = "doc"
                initializeDocView(uri.toString(), adapterFile)
            }

            isExcelFile(title) -> {
                fileType = "excel"
                initializeDocView(uri.toString(), adapterFile)
            }

            isPowerPointFile(title) -> {
                fileType = "ppt"
                initializeDocView(uri.toString(), adapterFile)
            }

            else -> {
                // Try to open with appropriate viewer based on MIME type
                tryOpenWithAutoDetect(uri, title)
            }
        }
    }

    private fun initializePdfView(pdfUri: Uri, adapterFile: Boolean = false) {
        try {
            showLoading()
            binding.pdfContainer.visibility = View.VISIBLE
            binding.docView.visibility = View.GONE
            binding.ivTick.visibility = View.GONE // Hide convert button initially

            // Get file from URI
            val file = GetPath.getFileFromUri(this, pdfUri)
            if (file != null && file.exists()) {
                currentFile = file
            }

            // Create PdfRendererView and add it to the container
            pdfView = PdfRendererView(this).apply {
                statusListener = object : PdfRendererView.StatusCallBack {
                    override fun onPdfLoadStart() {
                        runOnUiThread {
                            showLoading()
                            binding.ivTick.visibility = View.GONE
                        }
                    }

                    override fun onPdfLoadSuccess(absolutePath: String) {
                        runOnUiThread {
                            hideLoading()
                            // Show convert button when PDF is loaded
                            binding.ivTick.isVisible = !adapterFile
                            // Update file reference
                            //currentFile = File(absolutePath)
                        }
                    }

                    override fun onError(error: Throwable) {
                        runOnUiThread {
                            hideLoading()
                            binding.ivTick.visibility = View.GONE
                            showError(error.message ?: "Failed to load PDF")
                        }
                    }

                    override fun onPageChanged(currentPage: Int, totalPage: Int) {
                        runOnUiThread { updatePageInfo(currentPage, totalPage) }
                    }
                }

                // Initialize with SAF/content Uri
                initWithUri(pdfUri)
            }

            binding.pdfContainer.removeAllViews()
            binding.pdfContainer.addView(pdfView)

        } catch (e: Exception) {
            hideLoading()
            binding.ivTick.visibility = View.GONE
            showError("Error initializing PDF viewer: ${e.message}")
        }
    }


    private fun initializeDocView(fileUri: String, adapterFile: Boolean = false) {
        try {
            showLoading()
            binding.pdfContainer.visibility = View.GONE
            binding.docView.visibility = View.VISIBLE
            binding.ivTick.isVisible = !adapterFile

            val uri = fileUri.toUri()
            var finalFile: File? = null

            // 1. Try to get file directly
            val file = GetPath.getFileFromUri(this, uri)
            if (file != null && file.exists()) {
                finalFile = file
            }

            // 2. If not found or not accessible, try to copy from stream
            if (finalFile == null) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val title = binding.tvTitle.text.toString()
                        // Ensure extension
                        val extension = if (title.contains(".")) title.substringAfterLast(".") else "doc"
                        val tempFile = File(cacheDir, "temp_view_${System.currentTimeMillis()}.$extension")
                        
                        tempFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        finalFile = tempFile
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            currentFile = finalFile

            if (finalFile != null && finalFile.exists()) {
                docView = binding.docView
                // ALWAYS use PATH and the absolute path of the local file
                docView?.openDoc(this, finalFile.absolutePath, DocSourceType.PATH)
            } else {
                showError("Unable to load file")
            }

            hideLoading()

        } catch (e: Exception) {
            hideLoading()
            binding.ivTick.visibility = View.GONE
            showError("Error initializing document viewer: ${e.message}")
        }
    }

    private fun tryOpenWithAutoDetect(uri: Uri, fileName: String) {
        val file = GetPath.getFileFromUri(this, uri)

        if (file == null || !file.exists()) {
            showError("Unable to access file")
            return
        }

        // Try to open as PDF first (some files might be PDF even with wrong extension)
        try {
            showLoading()
            initializePdfView(uri, false)
        } catch (e: Exception) {
            // If PDF fails, try with DocView
            try {
                hideLoading()
                showLoading()
                initializeDocView(uri.toString(), false)
            } catch (e2: Exception) {
                hideLoading()
                showError("Unsupported file format: $fileName")
            }
        }
    }

    private fun updatePageInfo(currentPage: Int, totalPages: Int) {
        if (totalPages > 0) {
            binding.toolbar.subtitle = "Page ${currentPage + 1} of $totalPages"
        } else {
            binding.toolbar.subtitle = "Document"
        }
    }

    private fun showError(message: String) {
        hideLoading()
        binding.layoutError.root.visibility = View.VISIBLE
        binding.layoutError.tvErrorMessage.text = message

        binding.layoutError.btnRetry.setClickWithTimeout {
            binding.layoutError.root.visibility = View.GONE
            showLoading()
            loadDocument()
        }
    }

    private fun showLoading() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.layoutError.root.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingLayout.visibility = View.GONE
    }

    private fun isPdfFile(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        return name.lowercase().endsWith(".pdf")
    }

    private fun isDocFile(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val lower = name.lowercase()
        return lower.endsWith(".doc") || lower.endsWith(".docx")
    }

    private fun isExcelFile(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val lower = name.lowercase()
        return lower.endsWith(".xls") || lower.endsWith(".xlsx") ||
                lower.endsWith(".csv")
    }

    private fun isPowerPointFile(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val lower = name.lowercase()
        return lower.endsWith(".ppt") || lower.endsWith(".pptx")
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfView?.closePdfRender()
        docView = null
        pdfView = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    companion object {
        private var currentFileUri: Uri? = null
    }
}