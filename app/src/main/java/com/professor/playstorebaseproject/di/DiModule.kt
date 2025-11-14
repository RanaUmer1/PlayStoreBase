package com.professor.playstorebaseproject.di

/**

Created by Umer Javed
Senior Android Developer
Email: umerr8019@gmail.com

 */
import android.app.Application
import android.content.Context
import androidx.room.Room
import com.mzalogics.ads.domain.core.AdMobManager
import com.professor.playstorebaseproject.Constants
import com.professor.playstorebaseproject.data.db.AppDatabase
import com.professor.playstorebaseproject.data.db.DataModelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DiModule {

    @Provides
    @Singleton
    fun provideAdMobManager(application: Application): AdMobManager {
        return AdMobManager.getInstance(application)
    }


    @Module
    @InstallIn(SingletonComponent::class)
    object DatabaseModule {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context, AppDatabase::class.java, Constants.DB_NAME
            ).build()
        }

        @Provides
        fun provideFavoriteDao(db: AppDatabase): DataModelDao = db.favoriteDao()
    }

}