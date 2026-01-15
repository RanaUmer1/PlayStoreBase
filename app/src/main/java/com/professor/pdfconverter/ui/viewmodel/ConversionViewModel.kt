package com.professor.pdfconverter.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.professor.pdfconverter.data.source.api.ConversionResponse
import com.professor.pdfconverter.data.use_case.ConvertFileUseCase
import com.professor.pdfconverter.data.use_case.DownloadFileUseCase
import com.professor.pdfconverter.data.use_case.LoadFileUseCase
import com.professor.pdfconverter.enums.DownloadState
import com.professor.pdfconverter.utils.ApiResult

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class ConversionViewModel @Inject constructor(
    private val convertFileUseCase: ConvertFileUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val loadFileUseCase: LoadFileUseCase
) : ViewModel() {

    private val _conversionState = MutableLiveData<ApiResult<ConversionResponse>?>()
    val conversionState: LiveData<ApiResult<ConversionResponse>?> = _conversionState

    private val _fileStatus = MutableLiveData<Boolean>()
    val fileStatus: LiveData<Boolean> = _fileStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _downloadState = MutableLiveData<DownloadState>()
    val downloadState: LiveData<DownloadState> = _downloadState

    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> = _downloadProgress

    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress

    fun setFileReady(isReady: Boolean) {
        _fileStatus.value = isReady
    }

    fun loadFileFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = loadFileUseCase.loadFileFromUri(context, uri)
                _fileStatus.value = success
            } catch (e: Exception) {
                _fileStatus.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun convertFile(filePart: MultipartBody.Part, conversionType: String) {
        viewModelScope.launch {
            convertFileUseCase(filePart, conversionType)
                .collect { result ->
                    _conversionState.value = result
                }
        }
    }

    fun downloadFile(downloadUrl: String, fileName: String? = null) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Loading(0)

            val result = downloadFileUseCase(downloadUrl, fileName) { progress ->
                _downloadProgress.postValue(progress)
            }

            when {
                result.success -> {
                    _downloadState.value = DownloadState.Success(
                        filePath = result.filePath!!,
                        fileUri = result.fileUri
                    )
                }
                else -> {
                    _downloadState.value = DownloadState.Error(
                        message = result.errorMessage ?: "Download failed"
                    )
                }
            }
        }
    }

    // Alternative method with callback (for backward compatibility)
    fun downloadFile(
        downloadUrl: String,
        fileName: String? = null,
        callback: (Boolean, String, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = downloadFileUseCase(downloadUrl, fileName) { progress ->
                _downloadProgress.postValue(progress)
            }

            if (result.success) {
                callback(true, "File downloaded: ${result.filePath}","${result.filePath}")
            } else {
                callback(false, result.errorMessage ?: "Download failed","")
            }
        }
    }

    fun updateDownloadProgress(progress: Int) {
        _downloadProgress.value = progress
        _downloadState.value = DownloadState.Loading(progress)
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
        _downloadProgress.value = 0
        _uploadProgress.value = 0
    }
    
    fun updateUploadProgress(progress: Int) {
        _uploadProgress.postValue(progress)
    }
}