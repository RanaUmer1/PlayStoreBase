package com.professor.pdfconverter.adapter

import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import com.professor.pdfconverter.databinding.ItemPdfPageBinding

class PdfPageAdapter(private val pdfRenderer: PdfRenderer) :
    RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val binding = ItemPdfPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PdfPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = pdfRenderer.pageCount

    inner class PdfPageViewHolder(private val binding: ItemPdfPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val page = pdfRenderer.openPage(position)

            // Create a bitmap with the page's dimensions
            // We can scale it down if needed for performance, but for now let's use actual size
            // or a fixed width to fit screen.
            // For better performance, we should calculate the optimal size based on screen width.

            val width = binding.root.resources.displayMetrics.widthPixels
            val scale = width.toFloat() / page.width
            val height = (page.height * scale).toInt()

            val bitmap = createBitmap(width, height)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            binding.ivPage.setImageBitmap(bitmap)

            page.close()
        }
    }
}