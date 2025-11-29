package com.professor.pdfconverter.model

/**
 * Simple model for recent files shown on the main screen
 */
data class RecentFileModel(
    val id: Long,
    val name: String,
    val date: String,
    val time: String,
    val size: String,
    val fileType: FileType
)

enum class FileType {
    WORD,
    PDF,
}
