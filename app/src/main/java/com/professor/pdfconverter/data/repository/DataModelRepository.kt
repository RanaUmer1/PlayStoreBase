package com.professor.pdfconverter.data.repository

import com.professor.pdfconverter.data.db.DataModelDao
import com.professor.pdfconverter.model.DataModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataModelRepository @Inject constructor(
    private val dataModelDao: DataModelDao
) {
    suspend fun addFavorite(model: DataModel) = dataModelDao.insertData(model)
}
