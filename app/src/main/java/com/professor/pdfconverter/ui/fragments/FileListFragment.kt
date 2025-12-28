package com.professor.pdfconverter.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.adapter.RecentFilesAdapter
import com.professor.pdfconverter.databinding.FragmentFileListBinding
import com.professor.pdfconverter.model.FileType
import com.professor.pdfconverter.model.RecentFileModel
import com.professor.pdfconverter.ui.screens.DocumentViewerActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileListFragment : Fragment() {

    private var _binding: FragmentFileListBinding? = null
    private val binding get() = _binding!!
    private var fileType: FileType = FileType.PDF

    companion object {
        private const val ARG_FILE_TYPE = "arg_file_type"

        fun newInstance(fileType: FileType): FileListFragment {
            val fragment = FileListFragment()
            val args = Bundle()
            args.putSerializable(ARG_FILE_TYPE, fileType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fileType = it.getSerializable(ARG_FILE_TYPE) as FileType
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
    }

    override fun onResume() {
        super.onResume()
        setupAdapter()
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

    private var recentFilesAdapter: RecentFilesAdapter? = null

    private fun setupAdapter() {
        if (recentFilesAdapter == null) {
            recentFilesAdapter = RecentFilesAdapter(
                onItemClick = { file ->
                    openFileInViewer(file)
                },
                onMoreClick = {}
            )
            binding.rvFiles.adapter = recentFilesAdapter
            binding.rvFiles.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        }
        
        val list = getFiles()
        recentFilesAdapter?.submitList(list)

        if (list.isEmpty()) {
            binding.layoutNoData.visibility = View.VISIBLE
            binding.rvFiles.visibility = View.GONE
        } else {
            binding.layoutNoData.visibility = View.GONE
            binding.rvFiles.visibility = View.VISIBLE
        }
    }

    private fun getFiles(): List<RecentFileModel> {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "PDFConverter"
        )

        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        val files = directory.listFiles() ?: return emptyList()

        return files.filter {
            if (fileType == FileType.PDF) {
                it.extension.equals("pdf", ignoreCase = true)
            } else {
                it.extension.equals("doc", ignoreCase = true) || it.extension.equals(
                    "docx",
                    ignoreCase = true
                )
            }
        }.sortedByDescending { it.lastModified() }
            .mapIndexed { index, file ->
                val lastModified = Date(file.lastModified())
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                val fileSize = if (file.length() > 1024 * 1024) {
                    String.format("%.1f MB", file.length() / (1024.0 * 1024.0))
                } else {
                    String.format("%.1f KB", file.length() / 1024.0)
                }

                RecentFileModel(
                    id = index.toLong(),
                    name = file.name,
                    date = dateFormat.format(lastModified),
                    time = timeFormat.format(lastModified),
                    size = fileSize,
                    uri = Uri.fromFile(file),
                    path = file.absolutePath,
                    fileType = fileType
                )
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        recentFilesAdapter = null
    }
}
