package com.professor.pdfconverter.data.use_case

import android.content.Context
import android.os.Environment
import androidx.core.content.FileProvider
import com.professor.pdfconverter.data.repository.ConversionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(
    private val repository: ConversionRepository,
    @ApplicationContext private val context: Context
) {
    
    data class DownloadResult(
        val success: Boolean,
        val filePath: String? = null,
        val errorMessage: String? = null,
        val fileUri: android.net.Uri? = null
    )

    suspend operator fun invoke(
        downloadUrl: String,
        fileName: String? = null
    ): DownloadResult {
        return try {
            // Get response from repository
            val response: Response<ResponseBody> = repository.downloadFile(downloadUrl)
            
            if (response.isSuccessful && response.body() != null) {
                // Generate filename if not provided
                val actualFileName = fileName ?: generateFileName(downloadUrl)
                
                // Save file
                val result = saveFile(response.body()!!, actualFileName)
                
                if (result.success) {
                    // Get URI for the saved file (for sharing/opening)
                    val fileUri = getFileUri(result.filePath!!)
                    DownloadResult(
                        success = true,
                        filePath = result.filePath,
                        fileUri = fileUri
                    )
                } else {
                    DownloadResult(
                        success = false,
                        errorMessage = result.errorMessage
                    )
                }
            } else {
                DownloadResult(
                    success = false,
                    errorMessage = "Download failed: ${response.code()} ${response.message()}"
                )
            }
        } catch (e: Exception) {
            DownloadResult(
                success = false,
                errorMessage = "Download error: ${e.message}"
            )
        }
    }

    private suspend fun saveFile(body: ResponseBody, fileName: String): SaveFileResult {
        return withContext(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            
            try {
                // Create downloads directory if it doesn't exist
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "PDFConverter"
                )
                
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                // Create file
                val file = File(downloadsDir, fileName)
                
                // Check if file already exists, append number if needed
                val finalFile = getUniqueFile(file)
                
                // Write file
                inputStream = body.byteStream()
                outputStream = FileOutputStream(finalFile)
                
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L
                val totalBytes = body.contentLength()
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    // You could emit progress here if needed
                    // val progress = (totalBytesRead * 100 / totalBytes).toInt()
                }
                
                outputStream.flush()
                
                SaveFileResult(
                    success = true,
                    filePath = finalFile.absolutePath,
                    fileSize = finalFile.length()
                )
            } catch (e: Exception) {
                SaveFileResult(
                    success = false,
                    errorMessage = "Save failed: ${e.message}"
                )
            } finally {
                inputStream?.close()
                outputStream?.close()
                body.close()
            }
        }
    }

    private fun getUniqueFile(file: File): File {
        if (!file.exists()) return file
        
        val nameWithoutExt = file.nameWithoutExtension
        val extension = file.extension
        
        var counter = 1
        var newFile: File
        
        do {
            val newName = "$nameWithoutExt ($counter).$extension"
            newFile = File(file.parent, newName)
            counter++
        } while (newFile.exists())
        
        return newFile
    }

    private fun generateFileName(downloadUrl: String): String {
        return try {
            val url = java.net.URL(downloadUrl)
            val path = url.path
            if (path.isNotEmpty()) {
                val fileName = path.substringAfterLast("/")
                if (fileName.isNotEmpty()) {
                    fileName
                } else {
                    "converted_${System.currentTimeMillis()}.pdf"
                }
            } else {
                "converted_${System.currentTimeMillis()}.pdf"
            }
        } catch (e: Exception) {
            "converted_${System.currentTimeMillis()}.pdf"
        }
    }

    private fun getFileUri(filePath: String): android.net.Uri? {
        return try {
            val file = File(filePath)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    private data class SaveFileResult(
        val success: Boolean,
        val filePath: String? = null,
        val errorMessage: String? = null,
        val fileSize: Long = 0L
    )
}