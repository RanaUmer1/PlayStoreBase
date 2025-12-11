package com.professor.pdfconverter.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.professor.pdfconverter.R
import com.professor.pdfconverter.databinding.FragmentConvertedBinding
import com.professor.pdfconverter.model.FileType
import com.professor.pdfconverter.utils.setClickWithTimeout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConvertedFragment : Fragment() {

    private var _binding: FragmentConvertedBinding? = null
    private val binding get() = _binding!!

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
        setupViewPager()
    }

    private fun listeners() {
        binding.tabPdf.setClickWithTimeout {
            binding.viewPager.currentItem = 0
        }

        binding.tabWord.setClickWithTimeout {
            binding.viewPager.currentItem = 1
        }
    }

    private fun setupViewPager() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) {
                    FileListFragment.newInstance(FileType.PDF)
                } else {
                    FileListFragment.newInstance(FileType.WORD)
                }
            }
        }
        binding.viewPager.adapter = adapter
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabs(position == 0)
            }
        })
    }

    private fun updateTabs(isPdf: Boolean) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_color_secondary)
        val transparent = ContextCompat.getColor(requireContext(), android.R.color.transparent)

        if (isPdf) {
            binding.tvPdf.setTextColor(primaryColor)
            binding.indicatorPdf.setBackgroundColor(primaryColor)
            binding.tvWord.setTextColor(secondaryColor)
            binding.indicatorWord.setBackgroundColor(transparent)
        } else {
            binding.tvPdf.setTextColor(secondaryColor)
            binding.indicatorPdf.setBackgroundColor(transparent)
            binding.tvWord.setTextColor(primaryColor)
            binding.indicatorWord.setBackgroundColor(primaryColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
