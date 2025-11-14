package com.professor.playstorebaseproject.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.professor.playstorebaseproject.model.DataModel

@Database(entities = [DataModel::class], version = 3)
//@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): DataModelDao
}
