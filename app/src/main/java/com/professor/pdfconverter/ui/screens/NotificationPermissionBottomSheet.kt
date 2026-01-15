package com.professor.pdfconverter.ui.screens

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.professor.pdfconverter.R
import com.professor.pdfconverter.databinding.NotificationPermissionBottomSheetBinding
import com.professor.pdfconverter.ui.screens.MainActivity.Companion.NOTIFICATION_PERMISSION_CODE
import com.professor.pdfconverter.utils.setClickWithTimeout

class NotificationPermissionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: NotificationPermissionBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NotificationPermissionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAllow.setClickWithTimeout {
            dismiss()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        binding.btnMaybeLater.setClickWithTimeout {
            dismiss()
        }
        binding.ivCross.setClickWithTimeout {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog
}
