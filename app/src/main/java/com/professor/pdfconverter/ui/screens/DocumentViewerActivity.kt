package com.professor.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.widget.DocView
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.databinding.ActivityDocumentViewerBinding
import com.professor.pdfconverter.utils.GetPath
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

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        binding.ivTick.setOnClickListener {
            openDocumentLoadActivity()
        }
    }

    private fun openDocumentLoadActivity() {
        currentFile?.let { file ->
            val intent = Intent(this, DocumentLoadActivity::class.java).apply {
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
        val uriString = intent.getStringExtra(Constants.EXTRA_PDF_URI)
        val title = intent.getStringExtra(Constants.EXTRA_PDF_NAME) ?: "Document"

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
                initializePdfView(uri)
            }

            isDocFile(title) -> {
                fileType = "doc"
                initializeDocView(uri)
            }

            isExcelFile(title) -> {
                fileType = "excel"
                initializeDocView(uri)
            }

            isPowerPointFile(title) -> {
                fileType = "ppt"
                initializeDocView(uri)
            }

            else -> {
                // Try to open with appropriate viewer based on MIME type
                tryOpenWithAutoDetect(uri, title)
            }
        }
    }

    private fun initializePdfView(pdfUri: Uri) {
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
                            binding.ivTick.visibility = View.VISIBLE
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

    private fun initializeDocView(fileUri: Uri) {
        try {
            showLoading()
            binding.pdfContainer.visibility = View.GONE
            binding.docView.visibility = View.VISIBLE
            binding.ivTick.visibility = View.GONE // Hide convert button initially

            // Get file from URI
            val file = GetPath.getFileFromUri(this, fileUri)
            if (file != null && file.exists()) {
                currentFile = file
            }

            docView = binding.docView
            docView?.openDoc(this, file?.absolutePath, DocSourceType.PATH)
            binding.ivTick.isVisible = true
            // Let the library handle its own internal loading UI
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
            initializePdfView(uri)
        } catch (e: Exception) {
            // If PDF fails, try with DocView
            try {
                hideLoading()
                showLoading()
                initializeDocView(uri)
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

        binding.layoutError.btnRetry.setOnClickListener {
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