package com.professor.pdfconverter.data.repository

import com.professor.pdfconverter.data.source.api.ApiService
import com.professor.pdfconverter.data.source.api.ConversionResponse
import com.professor.pdfconverter.utils.ApiResult
import com.professor.pdfconverter.utils.NetworkHelper
import com.professor.pdfconverter.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversionRepository @Inject constructor(
    private val apiService: ApiService,
    private val networkHelper: NetworkHelper
) {

    fun convertPdfToDoc(
        filePart: MultipartBody.Part
    ): Flow<ApiResult<ConversionResponse>> = flow {
        emit(ApiResult.loading())

        if (!networkHelper.isNetworkConnected()) {
            emit(ApiResult.error("No internet connection"))
            return@flow
        }

        val maxRetries = 3
        var currentAttempt = 0
        var lastException: Exception? = null

        while (currentAttempt < maxRetries) {
            try {
                val response = apiService.convertPdfToDoc(
                    filePart,
                    BuildConfig.API_KEY
                )

                if (response.isSuccessful && response.body()?.code == "200") {
                    val data = response.body()
                    if (data != null) {
                        emit(ApiResult.success(data))
                        return@flow
                    } else {
                        emit(ApiResult.error("Conversion failed: No data received"))
                        return@flow
                    }
                } else {
                    // Check for 504 Gateway Timeout
                    if (response.code() == 504 && currentAttempt < maxRetries - 1) {
                        currentAttempt++
                        val delayMs = (1000L * (1 shl currentAttempt)) // 2s, 4s, 8s
                        kotlinx.coroutines.delay(delayMs)
                        continue
                    }
                    
                    val errorMessage = response.body()?.code
                        ?: response.message()
                        ?: "Conversion failed"
                    emit(ApiResult.error(errorMessage, response.code()))
                    return@flow
                }
            } catch (e: Exception) {
                lastException = e
                // Retry on timeout exceptions
                if ((e is java.net.SocketTimeoutException || 
                     e.message?.contains("timeout", ignoreCase = true) == true) && 
                    currentAttempt < maxRetries - 1) {
                    currentAttempt++
                    val delayMs = (1000L * (1 shl currentAttempt))
                    kotlinx.coroutines.delay(delayMs)
                    continue
                } else {
                    emit(ApiResult.error(e.message ?: "Unknown error occurred"))
                    return@flow
                }
            }
        }
        
        emit(ApiResult.error(lastException?.message ?: "Conversion failed after $maxRetries attempts"))
    }

    fun convertDocToPdf(
        filePart: MultipartBody.Part
    ): Flow<ApiResult<ConversionResponse>> = flow {
        emit(ApiResult.loading())

        if (!networkHelper.isNetworkConnected()) {
            emit(ApiResult.error("No internet connection"))
            return@flow
        }

        try {
            val response =
                apiService.convertDocToPdf(filePart, BuildConfig.API_KEY)

            if (response.isSuccessful && response.body()?.code == "200") {
                val data = response.body()
                if (data != null) {
                    emit(ApiResult.success(data))
                } else {
                    emit(ApiResult.error("Conversion failed: No data received"))
                }
            } else {
                val errorMessage = response.body()?.code
                    ?: response.message()
                    ?: "Conversion failed"
                emit(ApiResult.error(errorMessage, response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.error(e.message ?: "Unknown error occurred"))
        }
    }

    suspend fun downloadFile(downloadUrl: String): Response<ResponseBody> {
        return apiService.downloadFile(downloadUrl)
    }

    // Add file download with progress tracking
    suspend fun downloadFileWithProgress(
        downloadUrl: String,
        onProgress: (percent: Int) -> Unit
    ): Flow<ApiResult<String>> = flow {
        emit(ApiResult.loading())

        if (!networkHelper.isNetworkConnected()) {
            emit(ApiResult.error("No internet connection"))
            return@flow
        }

        try {
            val response = apiService.downloadFile(downloadUrl)

            if (response.isSuccessful) {
                // Note: For actual progress tracking, you'd need to intercept
                // the OkHttp response. This is simplified.
                emit(ApiResult.success("Download started"))
            } else {
                emit(ApiResult.error("Download failed: ${response.message()}", response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.error(e.message ?: "Download error"))
        }
    }


}