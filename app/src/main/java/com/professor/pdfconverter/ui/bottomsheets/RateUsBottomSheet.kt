package com.professor.pdfconverter.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.professor.pdfconverter.databinding.BottomSheetRateUsBinding
import com.professor.pdfconverter.utils.setClickWithTimeout

class RateUsBottomSheet(
    private val onRateClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRateUsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRateUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.ivClose.setClickWithTimeout {
            dismiss()
        }

        binding.btnRate.setClickWithTimeout {
            dismiss()
            onRateClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
