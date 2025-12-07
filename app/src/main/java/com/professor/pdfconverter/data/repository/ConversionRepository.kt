package com.professor.pdfconverter.data.repository

import com.professor.pdfconverter.data.source.api.ApiService
import com.professor.pdfconverter.data.source.api.ConversionResponse
import com.professor.pdfconverter.utils.ApiResult
import com.professor.pdfconverter.utils.NetworkHelper
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
        
        try {

            val response = apiService.convertPdfToDoc(filePart,
                "public_key_048771f35b4e69cae519adfabc77b30b")
            
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
    
     fun convertDocToPdf(
        filePart: MultipartBody.Part
    ): Flow<ApiResult<ConversionResponse>> = flow {
        emit(ApiResult.loading())
        
        if (!networkHelper.isNetworkConnected()) {
            emit(ApiResult.error("No internet connection"))
            return@flow
        }
        
        try {

            val response = apiService.convertDocToPdf(filePart)
            
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