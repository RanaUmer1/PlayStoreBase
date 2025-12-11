package com.professor.pdfconverter.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.adapter.RecentFilesAdapter
import com.professor.pdfconverter.databinding.FragmentHomeBinding
import com.professor.pdfconverter.model.FileType
import com.professor.pdfconverter.model.RecentFileModel
import com.professor.pdfconverter.utils.Utils
import com.professor.pdfconverter.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint
import android.os.Environment
import com.professor.pdfconverter.ui.screens.DocumentViewerActivity
import com.professor.pdfconverter.ui.screens.PremiumActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var pdfPickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private lateinit var docPickerLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setupFilePicker()
        listeners()
    }

    override fun onResume() {
        super.onResume()
        setupAdapter()
    }

    private fun setupFilePicker() {
        // PDF picker
        pdfPickerLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { openViewerActivity(it) }
        }

        // Office documents picker (DOC, DOCX, XLS, XLSX, PPT, PPTX)
        docPickerLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let { openViewerActivity(it) }
        }
    }

    private fun init() {
        setupAdapter()
    }

    private fun listeners() {
        binding.cardRemoveAds.setClickWithTimeout {
            startActivity(Intent(requireContext(), PremiumActivity::class.java))
        }

        binding.pdfToDocContainer.setClickWithTimeout {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }

        binding.rightContainer.setClickWithTimeout {
            // Pick Office documents (Word, Excel, PowerPoint)
            docPickerLauncher.launch(
                arrayOf(
                    // Word
                    "application/msword", // .doc
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx

                    // Excel
                    "application/vnd.ms-excel", // .xls
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx

                    // PowerPoint
                    "application/vnd.ms-powerpoint", // .ppt
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation" // .pptx
                )
            )
        }
    }

    private fun setupAdapter() {
        val adapter = RecentFilesAdapter(
            onItemClick = { file ->
                openFileInViewer(file)

            },
            onMoreClick = { file ->
                // Handle more click
            }
        )
        binding.rvRecentFiles.adapter = adapter
        binding.rvRecentFiles.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        val list = getRecentFiles()
        adapter.submitList(list)

        if (list.isEmpty()) {
            binding.layoutNoData.visibility = View.VISIBLE
            binding.rvRecentFiles.visibility = View.GONE
        } else {
            binding.layoutNoData.visibility = View.GONE
            binding.rvRecentFiles.visibility = View.VISIBLE
        }
    }

    private fun openFileInViewer(file: RecentFileModel) {
        val filePath = file.path ?: return

        // Convert file path to content URI using FileProvider
        val fileUri = if (filePath.startsWith("content://")) {
            Uri.parse(filePath)
        } else {
            // Use FileProvider to get content URI
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                File(filePath)
            )
        }

        val intent = Intent(requireContext(), DocumentViewerActivity::class.java).apply {
            // Pass URI, not file path
            putExtra(Constants.EXTRA_FILE_URI, fileUri.toString())
            putExtra(Constants.EXTRA_FILE_NAME, file.name)
            putExtra(Constants.EXTRA_FILE_VIEW_FROM_ADAPTER, true)
            // Add read permission
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }


    private fun getRecentFiles(): List<RecentFileModel> {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "PDFConverter"
        )

        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        val files = directory.listFiles() ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }
            .take(10)
            .mapIndexed { index, file ->
                val lastModified = Date(file.lastModified())
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                val fileSize = if (file.length() > 1024 * 1024) {
                    String.format("%.1f MB", file.length() / (1024.0 * 1024.0))
                } else {
                    String.format("%.1f KB", file.length() / 1024.0)
                }

                val type = if (file.extension.equals(
                        "pdf",
                        ignoreCase = true
                    )
                ) FileType.PDF else FileType.WORD

                RecentFileModel(
                    id = index.toLong(),
                    name = file.name,
                    date = dateFormat.format(lastModified),
                    time = timeFormat.format(lastModified),
                    size = fileSize,
                    uri = Uri.fromFile(file),
                    path = file.absolutePath,
                    fileType = type
                )
            }
    }


    private fun openViewerActivity(uri: Uri, fileName: String? = null) {
        val intent = Intent(
            requireContext(),
            DocumentViewerActivity::class.java
        ).apply {
            putExtra(Constants.EXTRA_FILE_URI, uri.toString())
            putExtra(Constants.EXTRA_FILE_NAME, fileName ?: Utils.getFileNameFromUri(uri, requireContext()))
        }

        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
