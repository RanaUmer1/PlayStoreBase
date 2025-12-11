package com.professor.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.R
import com.professor.pdfconverter.databinding.ActivitySuccessBinding
import com.professor.pdfconverter.utils.Utils
import java.io.File

class SuccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySuccessBinding
    private var fileType: String = ""
    private var fileName: String = ""
    private var filePath: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpUI()
        setupToolbar()
        setupListeners()

    }

    private fun setUpUI() {

        filePath = intent.getStringExtra(Constants.EXTRA_FILE_PATH) ?: ""
        fileName = intent.getStringExtra(Constants.EXTRA_FILE_NAME) ?: ""
        fileType = intent.getStringExtra(Constants.EXTRA_FILE_TYPE) ?: ""
        binding.tvFileName.text = fileName



        if (fileType != "pdf") {
            binding.ivFileIcon.setImageResource(R.drawable.ic_doc_small)
            binding.loadingBar.setIndicatorColor(resources.getColor(R.color.primary_color))

        } else {
            binding.ivFileIcon.setImageResource(R.drawable.ic_pdf_small)
            binding.loadingBar.setIndicatorColor(resources.getColor(R.color.red))
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.ivBack.setOnClickListener {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }

    }

    private fun setupListeners() {
        binding.btnViewDoc.setOnClickListener {
            openFileInViewer(
                filePath, fileName
            )
        }


        binding.btnShareDoc.setOnClickListener {
            Utils.shareFile(fileName, filePath, this)

        }
    }

    private fun openFileInViewer(file: String, name: String) {
        val filePath = file ?: return

        // Convert file path to content URI using FileProvider
        val fileUri = if (filePath.startsWith("content://")) {
            Uri.parse(filePath)
        } else {
            // Use FileProvider to get content URI
            androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.provider", File(filePath)
            )
        }

        val intent = Intent(this, DocumentViewerActivity::class.java).apply {
            // Pass URI, not file path
            putExtra(Constants.EXTRA_FILE_URI, fileUri.toString())
            putExtra(Constants.EXTRA_FILE_NAME, name)
            putExtra(Constants.EXTRA_FILE_VIEW_FROM_ADAPTER, true)
            // Add read permission
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)


    }


    override fun onBackPressed() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }
}