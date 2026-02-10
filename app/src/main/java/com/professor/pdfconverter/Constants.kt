package com.professor.pdfconverter

/**

Created by Umer Javed
Senior Android Developer
Created on 12/06/2025 3:22 pm
Email: umerr8019@gmail.com

 */
object Constants {

    /*Api Constants*/

    const val BASE_URL = "https://api-server.compdf.com/server/v2/process/"
    const val PDF_TO_DOC = "pdf/docx"
    const val DOC_TO_PDF = "docx/pdf"
    const val CONTENT_TYPE_JSON = "application/json"
    const val API_KEY_HEADER = "X-API-Key"
    const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB

    /*----------------------------------*/

    const val IS_FROM_START = "is_from_start"
    const val IS_FROM_START_TO_PREMIUM = "is_from_start_to_premium"


    const val SKU_SUBSCRIPTION_WEEKLY = "item_week"
    const val SKU_SUBSCRIPTION_MONTHLY = "item_month"
    const val SKU_SUBSCRIPTION_YEARLY = "item_1y"

    const val FROM_ONBOARDING = "from_onboarding"
    const val FROM_PRO = "from_pro"


    //Intent Keys
    const val EXTRA_FILE_URI = "extra_file_uri"
    const val EXTRA_FILE_NAME = "extra_file_name"
    const val EXTRA_FILE_VIEW_FROM_ADAPTER = "extra_file_view_from_adapter"



    const val EXTRA_FILE_PATH = "extra_file_path"
    const val EXTRA_FILE_TYPE = "extra_file_type"

    // Conversion types
    const val CONVERSION_PDF_TO_WORD = "pdf_to_word"
    const val CONVERSION_WORD_TO_PDF = "word_to_pdf"
    const val CONVERSION_EXCEL_TO_PDF = "excel_to_pdf"
    const val CONVERSION_PPT_TO_PDF = "ppt_to_pdf"


    // Other constants...


    //Db
    const val DB_NAME = "app_db"


}