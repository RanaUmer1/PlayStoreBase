package com.professor.playstorebaseproject.data.repository

import com.professor.playstorebaseproject.data.db.DataModelDao
import com.professor.playstorebaseproject.model.DataModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataModelRepository @Inject constructor(
    private val dataModelDao: DataModelDao
) {
    suspend fun addFavorite(model: DataModel) = dataModelDao.insertData(model)



}
