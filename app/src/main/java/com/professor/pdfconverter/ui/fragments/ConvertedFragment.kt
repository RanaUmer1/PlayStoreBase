package com.professor.pdfconverter.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.professor.pdfconverter.R
import com.professor.pdfconverter.adapter.RecentFilesAdapter
import com.professor.pdfconverter.databinding.FragmentConvertedBinding
import com.professor.pdfconverter.model.FileType
import com.professor.pdfconverter.model.RecentFileModel
import com.professor.pdfconverter.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConvertedFragment : Fragment() {

    private var _binding: FragmentConvertedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecentFilesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConvertedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        listeners()
    }

    private fun init() {
        setupAdapter()
        updateTabs(true) // Default to PDF
    }

    private fun listeners() {
        binding.tabPdf.setClickWithTimeout {
            updateTabs(true)
        }

        binding.tabWord.setClickWithTimeout {
            updateTabs(false)
        }
    }

    private fun updateTabs(isPdf: Boolean) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_color_secondary)
        val transparent = ContextCompat.getColor(requireContext(), android.R.color.transparent)

        val filteredList = if (isPdf) {
            binding.tvPdf.setTextColor(primaryColor)
            binding.indicatorPdf.setBackgroundColor(primaryColor)
            binding.tvWord.setTextColor(secondaryColor)
            binding.indicatorWord.setBackgroundColor(transparent)
            getDummyFiles().filter { it.fileType == FileType.PDF }
        } else {
            binding.tvPdf.setTextColor(secondaryColor)
            binding.indicatorPdf.setBackgroundColor(transparent)
            binding.tvWord.setTextColor(primaryColor)
            binding.indicatorWord.setBackgroundColor(primaryColor)
            getDummyFiles().filter { it.fileType == FileType.WORD }
        }
        
        adapter.submitList(filteredList)
        
        if (filteredList.isEmpty()) {
            binding.layoutNoData.visibility = View.VISIBLE
            binding.rvConvertedFiles.visibility = View.GONE
        } else {
            binding.layoutNoData.visibility = View.GONE
            binding.rvConvertedFiles.visibility = View.VISIBLE
        }
    }

    private fun setupAdapter() {
        adapter = RecentFilesAdapter(
            onItemClick = { /* Handle click */ },
            onMoreClick = { /* Handle more */ }
        )
        binding.rvConvertedFiles.adapter = adapter
        binding.rvConvertedFiles.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
    }

    private fun getDummyFiles(): List<RecentFileModel> {
        return listOf(
            RecentFileModel(
                id = 1L,
                name = "invoice_2026-01-15.pdf",
                date = "21-04-2025",
                time = "08:22am",
                size = "298kb",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 2L,
                name = "report_Q3_2027.pdf",
                date = "08-11-2025",
                time = "01:37pm",
                size = "157kb",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 3L,
                name = "budget_proposal_fy28.pdf",
                date = "12-01-2025",
                time = "11:59am",
                size = "335kb",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 4L,
                name = "customer_agreement_v3.pdf",
                date = "03-06-2025",
                time = "06:01pm",
                size = "199kb",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 5L,
                name = "marketing_plan_2029.pdf",
                date = "29-09-2025",
                time = "09:45am",
                size = "275kb",
                fileType = FileType.PDF
            ),
            RecentFileModel(
                id = 6L,
                name = "project_specs.docx",
                date = "15-05-2025",
                time = "10:00am",
                size = "500kb",
                fileType = FileType.WORD
            ),
            RecentFileModel(
                id = 7L,
                name = "meeting_notes.docx",
                date = "20-05-2025",
                time = "02:30pm",
                size = "120kb",
                fileType = FileType.WORD
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
