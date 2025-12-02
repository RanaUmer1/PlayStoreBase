package com.professor.pdfconverter.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream

object GetPath {

    fun getFileFromUri(context: Context, uri: Uri): File? {
        val fileName = getFileName(context, uri) ?: return null
        val file = File(context.cacheDir, fileName)

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(file)

            val buffer = ByteArray(1024)
            var length: Int

            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }



    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOpenableColumns())
            }
        }
        return name
    }

    fun Cursor.getColumnIndexOpenableColumns(): Int {
        return getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
    }


    @SuppressLint("NewApi")
    fun getPath(context: Context, uri: Uri): String? {

        // Check if the Uri is a Document Uri
        if (DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            }

            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.removePrefix("raw:")
                }

                val contentUri = ContentUris.withAppendedId(
                    "content://downloads/public_downloads".toUri(),
                    id.toLongOrNull() ?: return null
                )

                return getDataColumn(context, contentUri, null, null)
            }

            // MediaProvider
            else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        }

        // If scheme is content
        else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        }

        // If scheme is file
        else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        return null
    }


    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        uri ?: return null
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndexOrThrow(column)
                    return it.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri) =
        "com.android.externalstorage.documents" == uri.authority

    private fun isDownloadsDocument(uri: Uri) =
        "com.android.providers.downloads.documents" == uri.authority

    private fun isMediaDocument(uri: Uri) =
        "com.android.providers.media.documents" == uri.authority

}