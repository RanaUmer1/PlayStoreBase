package com.professor.playstorebaseproject.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toUri
import com.professor.playstorebaseproject.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 12:54 pm
Email: umerr8019@gmail.com

 */
object Utils {


    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun downloadFile(context: Context, url: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

                val fileName = url.toUri().lastPathSegment ?: return@withContext null
                val file = File(context.cacheDir, fileName)

                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } catch (e: Exception) {
                Log.e("PAG", "Download failed: ${e.message}")
                null
            }
        }


    fun getBitmapFromDrawable(context: Context, drawableResId: Int): Bitmap {
        val bmp = BitmapFactory.decodeResource(
            context.resources,
            drawableResId
        )

        return bmp

    }

    fun flipBitmap(source: Bitmap, horizontal: Boolean = true): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                preScale(-1f, 1f) // flip horizontally
            } else {
                preScale(1f, -1f) // flip vertically
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }


    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun setWallpaper(bitmap: Bitmap, flag: Int, context: Context) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(bitmap, null, true, flag)
            Toast.makeText(context, "Wallpaper set", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    suspend fun downloadToLocalFile(context: Context, fileUrl: String, fileName: String): File {
        val file = File(context.getExternalFilesDir(null), fileName)

        withContext(Dispatchers.IO) {
            URL(fileUrl).openStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }

    fun getMIMEType(url: String?): String? {
        var mType: String? = null
        val mExtension: String = MimeTypeMap.getFileExtensionFromUrl(url)
        mType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mExtension)
        return mType
    }

    fun hideKeyboard(view: View) {
        val imm =
            view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }




    fun resetDefaultRingtones(context: Context) {
        try {
            // Reset Phone Ringtone to system default
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                Settings.System.DEFAULT_RINGTONE_URI
            )

            // Reset Notification sound to system default
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_NOTIFICATION,
                Settings.System.DEFAULT_NOTIFICATION_URI
            )

            // Reset Alarm sound to system default
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_ALARM,
                Settings.System.DEFAULT_ALARM_ALERT_URI
            )

            Toast.makeText(context, context.getString(R.string.reset_ring), Toast.LENGTH_SHORT)
                .show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}