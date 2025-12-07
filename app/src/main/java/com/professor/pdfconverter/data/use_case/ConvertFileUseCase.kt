package com.professor.pdfconverter.data.use_case

import com.professor.pdfconverter.Constants
import com.professor.pdfconverter.data.repository.ConversionRepository
import com.professor.pdfconverter.data.source.api.ConversionResponse
import com.professor.pdfconverter.utils.ApiResult
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import javax.inject.Inject

class ConvertFileUseCase @Inject constructor(
    private val repository: ConversionRepository
) {
    operator fun invoke(
        filePart: MultipartBody.Part,
        conversionType: String
    ): Flow<ApiResult<ConversionResponse>> {
        return when (conversionType) {
            Constants.CONVERSION_PDF_TO_WORD -> repository.convertPdfToDoc(filePart)
            Constants.CONVERSION_WORD_TO_PDF -> repository.convertDocToPdf(filePart)
            else -> repository.convertPdfToDoc(filePart)
        }
    }
}