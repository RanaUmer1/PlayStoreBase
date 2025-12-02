package com.professor.pdfconverter.ui.screens

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.cherry.lib.doc.bean.DocSourceType

import com.cherry.lib.doc.widget.DocView
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.databinding.ActivityPdfViewerBinding
import com.professor.pdfconverter.utils.GetPath
import com.rajat.pdfviewer.PdfRendererView

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pdfView: PdfRendererView? = null
    private var docView: DocView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
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
            // Optional: Add search functionality
            // if (isPdfFile) pdfView?.searchPDF()
            // else docView?.search() // if DocView supports search
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
        binding.tvTitle.text = title

        // Determine file type and load appropriate viewer
        when {
            isPdfFile(title) -> initializePdfView(uri)
            isDocFile(title) -> initializeDocView(uri)
            isExcelFile(title) -> initializeDocView(uri)
            isPowerPointFile(title) -> initializeDocView(uri)
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

            // Create PdfRendererView and add it to the container
            pdfView = PdfRendererView(this).apply {
                statusListener = object : PdfRendererView.StatusCallBack {
                    override fun onPdfLoadStart() {
                        runOnUiThread { showLoading() }
                    }

                    override fun onPdfLoadSuccess(absolutePath: String) {
                        runOnUiThread { hideLoading() }
                    }

                    override fun onError(error: Throwable) {
                        runOnUiThread {
                            hideLoading()
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
            showError("Error initializing PDF viewer: ${e.message}")
        }
    }

    private fun initializeDocView(fileUri: Uri) {
        try {
            showLoading()
            binding.pdfContainer.visibility = View.GONE
            binding.docView.visibility = View.VISIBLE

            // Get file from URI
            val file = GetPath.getFileFromUri(this, fileUri)

            if (file == null || !file.exists()) {
                hideLoading()
                showError("Unable to read document file")
                return
            }

            docView = binding.docView
            docView?.openDoc(this, file.absolutePath, DocSourceType.PATH)

            // Let the library handle its own internal loading UI
            hideLoading()

        } catch (e: Exception) {
            hideLoading()
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
        // Update toolbar with page information
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

    private fun isTextFile(name: String?): Boolean {
        if (name.isNullOrEmpty()) return false
        val lower = name.lowercase()
        return lower.endsWith(".txt") || lower.endsWith(".rtf")
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
        // Handle back press for PDF viewer navigation
        super.onBackPressed()
    }
}