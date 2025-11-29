package com.professor.pdfconverter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.professor.pdfconverter.R
import com.professor.pdfconverter.databinding.ItemRecentFileBinding
import com.professor.pdfconverter.model.FileType
import com.professor.pdfconverter.model.RecentFileModel
import com.professor.pdfconverter.utils.setClickWithTimeout

class RecentFilesAdapter(
    private val onItemClick: (RecentFileModel) -> Unit,
    private val onMoreClick: (RecentFileModel) -> Unit
) : ListAdapter<RecentFileModel, RecentFilesAdapter.RecentFileViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<RecentFileModel>() {
        override fun areItemsTheSame(oldItem: RecentFileModel, newItem: RecentFileModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecentFileModel, newItem: RecentFileModel): Boolean {
            return oldItem == newItem
        }
    }

    inner class RecentFileViewHolder(private val binding: ItemRecentFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentFileModel) {
            binding.tvFileName.text = item.name
            binding.tvFileMeta.text = "${item.date}  •  ${item.time}  •  ${item.size}"

            val iconRes = when (item.fileType) {
                FileType.WORD -> R.drawable.ic_file_type_doc
                FileType.PDF -> R.drawable.ic_file_type_pdf
            }
            binding.ivFileIcon.setImageResource(iconRes)

            binding.root.setClickWithTimeout { onItemClick(item) }
            binding.ivMore.setClickWithTimeout { onMoreClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentFileViewHolder {
        val binding = ItemRecentFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentFileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
