package com.professor.playstorebaseproject.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.professor.playstorebaseproject.model.DataModel

@Dao
interface DataModelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(data: DataModel)


}
