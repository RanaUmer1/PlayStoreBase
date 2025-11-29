package com.professor.pdfconverter.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.professor.pdfconverter.adapter.RecentFilesAdapter
import com.professor.pdfconverter.databinding.FragmentHomeBinding
import com.professor.pdfconverter.model.FileType
import com.professor.pdfconverter.model.RecentFileModel
import com.professor.pdfconverter.ui.screens.PremiumActivity
import com.professor.pdfconverter.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        listeners()
    }

    private fun init() {
        setupAdapter()
    }

    private fun listeners() {
        binding.cardRemoveAds.setClickWithTimeout {
            startActivity(Intent(requireContext(), PremiumActivity::class.java))
        }
        
        // Add other listeners here if needed for pdf_to_doc_container etc.
    }

    private fun setupAdapter() {
        val adapter = RecentFilesAdapter(
            onItemClick = { file ->
                // Handle file click
            },
            onMoreClick = { file ->
                // Handle more click
            }
        )
        binding.rvRecentFiles.adapter = adapter
        binding.rvRecentFiles.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        val list = getDummyRecentFiles()
        adapter.submitList(list)
        
        if (list.isEmpty()) {
            binding.layoutNoData.visibility = View.VISIBLE
            binding.rvRecentFiles.visibility = View.GONE
        } else {
            binding.layoutNoData.visibility = View.GONE
            binding.rvRecentFiles.visibility = View.VISIBLE
        }
    }

    private fun getDummyRecentFiles(): List<RecentFileModel> {
        return listOf(
            RecentFileModel(
                id = 1L,
                name = "Sample_Document_1.pdf",
                date = "28 Nov 2025",
                time = "10:15 AM",
                size = "1.2 MB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 2L,
                name = "Project_Proposal.docx",
                date = "27 Nov 2025",
                time = "05:42 PM",
                size = "850 KB",
                fileType = FileType.WORD
            ),
            RecentFileModel(
                id = 3L,
                name = "Invoice_2025_11.pdf",
                date = "26 Nov 2025",
                time = "09:03 AM",
                size = "560 KB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 1L,
                name = "Sample_Document_1.pdf",
                date = "28 Nov 2025",
                time = "10:15 AM",
                size = "1.2 MB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 2L,
                name = "Project_Proposal.docx",
                date = "27 Nov 2025",
                time = "05:42 PM",
                size = "850 KB",
                fileType = FileType.WORD
            ),
            RecentFileModel(
                id = 3L,
                name = "Invoice_2025_11.pdf",
                date = "26 Nov 2025",
                time = "09:03 AM",
                size = "560 KB",
                fileType = FileType.PDF
            ), RecentFileModel(
                id = 1L,
                name = "Sample_Document_1.pdf",
                date = "28 Nov 2025",
                time = "10:15 AM",
                size = "1.2 MB",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 2L,
                name = "Project_Proposal.docx",
                date = "27 Nov 2025",
                time = "05:42 PM",
                size = "850 KB",
                fileType = FileType.WORD
            ),
            RecentFileModel(
                id = 3L,
                name = "Invoice_2025_11.pdf",
                date = "26 Nov 2025",
                time = "09:03 AM",
                size = "560 KB",
                fileType = FileType.PDF
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
