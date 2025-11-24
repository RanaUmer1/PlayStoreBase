package com.professor.pdfconverter.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.professor.pdfconverter.model.DataModel

@Database(entities = [DataModel::class], version = 3)
//@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): DataModelDao
}
