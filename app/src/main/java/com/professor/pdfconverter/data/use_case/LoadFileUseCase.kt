package com.professor.pdfconverter.data.use_case

import android.content.Context
import android.net.Uri
import com.professor.pdfconverter.utils.GetPath

import javax.inject.Inject

class LoadFileUseCase @Inject constructor() {
     fun loadFileFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val file = GetPath.getFileFromUri(context, uri)
            file != null && file.exists() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }
}