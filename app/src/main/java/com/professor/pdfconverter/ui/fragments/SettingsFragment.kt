package com.professor.pdfconverter.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.professor.pdfconverter.R
import com.professor.pdfconverter.databinding.FragmentSettingsBinding
import com.professor.pdfconverter.ui.screens.LanguageActivity
import com.professor.pdfconverter.ui.screens.PremiumActivity
import com.professor.pdfconverter.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listeners()
    }

    private fun listeners() {


        binding.cardRate.setClickWithTimeout {
            showRateUsDialog()
        }

        binding.cardFeedback.setClickWithTimeout {
            openStorePage()
        }

        binding.cardShare.setClickWithTimeout {
            shareApp()
        }

        binding.cardPrivacy.setClickWithTimeout {
            val url = "https://ozi-apps.s3.us-west-2.amazonaws.com/RealAnimalSound_PrivacyPolicy.html"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        binding.cardLanguage.setClickWithTimeout {
             startActivity(Intent(requireContext(), LanguageActivity::class.java))
        }
    }

    private fun openStorePage() {
        val url = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, getString(R.string.shareApp))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name))
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    private fun showRateUsDialog() {
        val bottomSheet = com.professor.pdfconverter.ui.bottomsheets.RateUsBottomSheet {
            openStorePage()
        }
        bottomSheet.show(parentFragmentManager, "RateUsBottomSheet")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
