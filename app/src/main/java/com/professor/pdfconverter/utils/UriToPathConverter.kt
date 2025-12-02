//package com.professor.pdfconverter.utils
//
//import android.annotation.SuppressLint
//import android.content.ContentUris
//import android.content.Context
//import android.database.Cursor
//import android.net.Uri
//import android.os.Build
//import android.os.Environment
//import android.provider.DocumentsContract
//import android.provider.MediaStore
//import android.provider.OpenableColumns
//import android.util.Log
//import java.io.*
//import androidx.core.net.toUri
//
//object UriToPathConverter {
//
//    private const val TAG = "UriToPathConverter"
//
//    /**
//     * Main function to get file path from Uri
//     * @return File path string or null if can't resolve
//     */
//
//
//
//
//    @SuppressLint("NewApi")
//    fun getPath(context: Context, uri: Uri): String? {
//
//        // Check if the Uri is a Document Uri
//        if (DocumentsContract.isDocumentUri(context, uri)) {
//
//            // ExternalStorageProvider
//            if (isExternalStorageDocument(uri)) {
//                val docId = DocumentsContract.getDocumentId(uri)
//                val split = docId.split(":")
//                val type = split[0]
//
//                if ("primary".equals(type, ignoreCase = true)) {
//                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
//                }
//            }
//
//            // DownloadsProvider
//            else if (isDownloadsDocument(uri)) {
//                val id = DocumentsContract.getDocumentId(uri)
//                if (id.startsWith("raw:")) {
//                    return id.removePrefix("raw:")
//                }
//
//                val contentUri = ContentUris.withAppendedId(
//                    Uri.parse("content://downloads/public_downloads"),
//                    id.toLongOrNull() ?: return null
//                )
//
//                return getDataColumn(context, contentUri, null, null)
//            }
//
//            // MediaProvider
//            else if (isMediaDocument(uri)) {
//                val docId = DocumentsContract.getDocumentId(uri)
//                val split = docId.split(":")
//                val type = split[0]
//
//                var contentUri: Uri? = null
//                when (type) {
//                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//                }
//
//                val selection = "_id=?"
//                val selectionArgs = arrayOf(split[1])
//
//                return getDataColumn(context, contentUri, selection, selectionArgs)
//            }
//        }
//
//        // If scheme is content
//        else if ("content".equals(uri.scheme, ignoreCase = true)) {
//            return getDataColumn(context, uri, null, null)
//        }
//
//        // If scheme is file
//        else if ("file".equals(uri.scheme, ignoreCase = true)) {
//            return uri.path
//        }
//
//        return null
//    }
//
//
//
//
//    @SuppressLint("Range")
//    fun getDocFilePathFromUri(uri: Uri, context: Context): String? {
//        return try {
//            when (uri.scheme) {
//                "file" -> {
//                    // Direct file path for file:// scheme
//                    uri.path?.takeIf { it.endsWith(".doc", true) || it.endsWith(".docx", true) }
//                }
//
//                "content" -> {
//                    // For content:// scheme - try to get real path
//                    getRealPathForDoc(uri, context)
//                }
//
//                else -> null
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    @SuppressLint("Range")
//    private fun getRealPathForDoc(uri: Uri, context: Context): String? {
//        // Check if it's from MediaStore
//        if (uri.authority == MediaStore.AUTHORITY) {
//            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
//            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
//                if (cursor.moveToFirst()) {
//                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
//                    if (path != null && (path.endsWith(".doc", true) || path.endsWith(".docx", true))) {
//                        return path
//                    }
//                }
//            }
//        }
//
//        // Try for Downloads
//        if (uri.authority == "com.android.providers.downloads.documents") {
//            val id = DocumentsContract.getDocumentId(uri)
//            val contentUri = ContentUris.withAppendedId(
//                Uri.parse("content://downloads/public_downloads"),
//                id.toLong()
//            )
//
//            val projection = arrayOf(MediaStore.MediaColumns.DATA)
//            context.contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
//                if (cursor.moveToFirst()) {
//                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
//                    if (path != null && (path.endsWith(".doc", true) || path.endsWith(".docx", true))) {
//                        return path
//                    }
//                }
//            }
//        }
//
//        // For other providers, copy to cache
//        return copyDocToCache(uri, context)
//    }
//
//    private fun copyDocToCache(uri: Uri, context: Context): String? {
//        return try {
//            val fileName = getDocFileNameFromUri(uri, context)
//            val cacheDir = context.externalCacheDir ?: context.cacheDir
//            val file = File(cacheDir, fileName)
//
//            context.contentResolver.openInputStream(uri)?.use { inputStream ->
//                FileOutputStream(file).use { outputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            }
//
//            file.absolutePath
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    @SuppressLint("Range")
//    fun getDocFileNameFromUri(uri: Uri, context: Context): String {
//        return when (uri.scheme) {
//            "content" -> {
//                val cursor = context.contentResolver.query(uri, null, null, null, null)
//                cursor?.use {
//                    if (it.moveToFirst()) {
//                        val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
//                        displayName?.takeIf { name ->
//                            name.endsWith(".doc", true) || name.endsWith(".docx", true)
//                        } ?: "document_${System.currentTimeMillis()}.docx"
//                    } else {
//                        "document_${System.currentTimeMillis()}.docx"
//                    }
//                } ?: "document_${System.currentTimeMillis()}.docx"
//            }
//            "file" -> {
//                File(uri.path!!).name.takeIf { name ->
//                    name.endsWith(".doc", true) || name.endsWith(".docx", true)
//                } ?: "document_${System.currentTimeMillis()}.docx"
//            }
//            else -> "document_${System.currentTimeMillis()}.docx"
//        }
//    }
//
//    fun getPath(context: Context, uri: Uri): String? {
//        return try {
//            when {
//                // Check if Uri is a local file
//                isLocalFile(uri) -> uri.path
//
//                // Check if Uri is a document from Downloads/Documents provider
//                isDocumentUri(context, uri) -> getPathFromDocumentUri(context, uri)
//
//                // Check if Uri is from MediaStore
//                isMediaStoreUri(uri) -> getPathFromMediaStore(context, uri)
//
//                // Check if Uri is from Google Drive/Cloud
//                isGoogleDriveUri(uri) -> getPathFromGoogleDrive(context, uri)
//
//                // Fallback: Copy to cache and return cache path
//                else -> copyUriToCacheAndGetPath(context, uri)
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error getting path from Uri: ${e.message}")
//            null
//        }
//    }
//
//    /**
//     * Get File object from Uri (with fallback to cache)
//     */
//    fun getFile(context: Context, uri: Uri): File? {
//        val path = getPath(context, uri)
//        return if (path != null && File(path).exists()) {
//            File(path)
//        } else {
//            // Fallback: Copy to cache
//            copyUriToCache(context, uri)
//        }
//    }
//
//    /**
//     * Get file name from Uri
//     */
//    fun getFileName(context: Context, uri: Uri): String? {
//        var name: String? = null
//
//        // Try to get from OpenableColumns
//        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                if (displayNameIndex != -1) {
//                    name = cursor.getString(displayNameIndex)
//                }
//            }
//        }
//
//        // Fallback: Get from path
//        if (name.isNullOrEmpty()) {
//            uri.path?.let { path ->
//                name = path.substringAfterLast("/")
//            }
//        }
//
//        // Fallback: Generate name
//        if (name.isNullOrEmpty()) {
//            name = "file_${System.currentTimeMillis()}"
//        }
//
//        return name
//    }
//
//    /**
//     * Get file size from Uri
//     */
//    fun getFileSize(context: Context, uri: Uri): Long {
//        var size = 0L
//
//        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
//                if (sizeIndex != -1) {
//                    size = cursor.getLong(sizeIndex)
//                }
//            }
//        }
//
//        return size
//    }
//
//    // =================== PRIVATE METHODS ===================
//
//    private fun isLocalFile(uri: Uri): Boolean {
//        return uri.scheme == null || uri.scheme == "file"
//    }
//
//    private fun isDocumentUri(context: Context, uri: Uri): Boolean {
//        return DocumentsContract.isDocumentUri(context, uri)
//    }
//
//    private fun isMediaStoreUri(uri: Uri): Boolean {
//        return uri.authority == MediaStore.AUTHORITY
//    }
//
//    private fun isGoogleDriveUri(uri: Uri): Boolean {
//        return uri.authority?.contains("google") == true ||
//               uri.toString().contains("googledrive") ||
//               uri.toString().contains("google.com")
//    }
//
//    @SuppressLint("Range")
//    private fun getPathFromMediaStore(context: Context, uri: Uri): String? {
//        val projection = arrayOf(MediaStore.MediaColumns.DATA)
//        var path: String? = null
//
//        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
//            }
//        }
//
//        return path
//    }
//
//    @SuppressLint("Range")
//    private fun getPathFromDocumentUri(context: Context, uri: Uri): String? {
//        if (!DocumentsContract.isDocumentUri(context, uri)) {
//            return null
//        }
//
//        val documentId = DocumentsContract.getDocumentId(uri)
//        val split = documentId.split(":").toTypedArray()
//
//        return when {
//            // ExternalStorageProvider
//            isExternalStorageDocument(uri) -> {
//                val type = split[0]
//                if ("primary".equals(type, ignoreCase = true)) {
//                    return Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
//                } else {
//                    // Handle non-primary volumes
//                    getPathFromNonPrimaryVolume(context, split[0], split[1])
//                }
//            }
//
//            // DownloadsProvider
//            isDownloadsDocument(uri) -> {
//                val contentUri = ContentUris.withAppendedId(
//                    "content://downloads/public_downloads".toUri(),
//                    documentId.toLong()
//                )
//                getDataColumn(context, contentUri, null, null)
//            }
//
//            // MediaProvider
//            isMediaDocument(uri) -> {
//                val contentUri: Uri = when (split[0]) {
//                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//                    else -> MediaStore.Files.getContentUri("external")
//                }
//
//                val selection = "_id=?"
//                val selectionArgs = arrayOf(split[1])
//                getDataColumn(context, contentUri, selection, selectionArgs)
//            }
//
//            // Google Drive, Dropbox, etc.
//            isGoogleDriveDocument(uri) -> {
//                // For Google Drive, we need to download/copy the file
//                copyUriToCacheAndGetPath(context, uri)
//            }
//
//            else -> null
//        }
//    }
//
//    private fun getPathFromGoogleDrive(context: Context, uri: Uri): String? {
//        // Google Drive files need to be downloaded
//        return copyUriToCacheAndGetPath(context, uri)
//    }
//
//    @SuppressLint("Range")
//    private fun getDataColumn(
//        context: Context,
//        uri: Uri,
//        selection: String?,
//        selectionArgs: Array<String>?
//    ): String? {
//        val column = "_data"
//        val projection = arrayOf(column)
//
//        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                return cursor.getString(cursor.getColumnIndexOrThrow(column))
//            }
//        }
//
//        return null
//    }
//
//    private fun getPathFromNonPrimaryVolume(context: Context, type: String, relativePath: String): String? {
//        // For Android 4.4+
//        val volumes = context.externalCacheDirs
//        for (volume in volumes) {
//            val path = volume.absolutePath
//            if (path.contains(type, ignoreCase = true)) {
//                return "$path/$relativePath"
//            }
//        }
//        return null
//    }
//
//    // =================== URI TYPE CHECKERS ===================
//
//    private fun isExternalStorageDocument(uri: Uri): Boolean {
//        return uri.authority == "com.android.externalstorage.documents"
//    }
//
//    private fun isDownloadsDocument(uri: Uri): Boolean {
//        return uri.authority == "com.android.providers.downloads.documents"
//    }
//
//    private fun isMediaDocument(uri: Uri): Boolean {
//        return uri.authority == "com.android.providers.media.documents"
//    }
//
//    private fun isGoogleDriveDocument(uri: Uri): Boolean {
//        return uri.authority == "com.google.android.apps.docs.storage" ||
//               uri.authority == "com.google.android.apps.docs.storage.legacy"
//    }
//
//    // =================== CACHE METHODS ===================
//
//    fun copyUriToCache(context: Context, uri: Uri): File? {
//        return try {
//            val fileName = getFileName(context, uri)
//            val cacheDir = context.externalCacheDir ?: context.cacheDir
//            val file = File(cacheDir, fileName ?: "temp_${System.currentTimeMillis()}")
//
//            context.contentResolver.openInputStream(uri)?.use { inputStream ->
//                FileOutputStream(file).use { outputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            }
//
//            file
//        } catch (e: Exception) {
//            Log.e(TAG, "Error copying Uri to cache: ${e.message}")
//            null
//        }
//    }
//
//    private fun copyUriToCacheAndGetPath(context: Context, uri: Uri): String? {
//        return copyUriToCache(context, uri)?.absolutePath
//    }
//
//    // =================== ANDROID 10+ (SCOPED STORAGE) ===================
//
//    /**
//     * For Android 10+ with scoped storage, we need to copy files to app storage
//     */
//    fun handleScopedStorageUri(context: Context, uri: Uri): File? {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // Android 10+ - always copy to app's private storage
//            return copyToAppPrivateStorage(context, uri)
//        }
//        return null
//    }
//
//    private fun copyToAppPrivateStorage(context: Context, uri: Uri): File? {
//        return try {
//            val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
//            val appDir = File(context.filesDir, "documents")
//            if (!appDir.exists()) appDir.mkdirs()
//
//            val file = File(appDir, fileName)
//
//            context.contentResolver.openInputStream(uri)?.use { inputStream ->
//                FileOutputStream(file).use { outputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            }
//
//            file
//        } catch (e: Exception) {
//            Log.e(TAG, "Error copying to private storage: ${e.message}")
//            null
//        }
//    }
//
//    // =================== MIME TYPE & EXTENSION ===================
//
//    fun getMimeType(context: Context, uri: Uri): String? {
//        return context.contentResolver.getType(uri)
//    }
//
//    fun getFileExtension(context: Context, uri: Uri): String? {
//        val fileName = getFileName(context, uri)
//        return fileName?.substringAfterLast(".", "")
//    }
//
//    // =================== BATCH PROCESSING ===================
//
//    fun getMultipleFiles(context: Context, uris: List<Uri>): List<File> {
//        return uris.mapNotNull { getFile(context, it) }
//    }
//
//    fun cleanupTempFiles(context: Context) {
//        val cacheDir = context.externalCacheDir ?: context.cacheDir
//        cacheDir.listFiles()?.forEach { file ->
//            if (file.name.startsWith("temp_") || file.name.startsWith("file_")) {
//                file.delete()
//            }
//        }
//    }
//}