package io.github.abhik9.expirywatch.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.database.DefaultDataCallback
import io.github.abhik9.expirywatch.core.database.ExpiryWatchDatabase
import io.github.abhik9.expirywatch.core.database.dao.CategoryDao
import io.github.abhik9.expirywatch.core.database.dao.ItemDao
import io.github.abhik9.expirywatch.core.database.dao.LocationDao
import io.github.abhik9.expirywatch.core.database.dao.ProductDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): ExpiryWatchDatabase =
        Room.databaseBuilder(context, ExpiryWatchDatabase::class.java, ExpiryWatchDatabase.NAME)
            .addCallback(DefaultDataCallback(context.resources))
            .build()

    @Provides
    fun providesItemDao(database: ExpiryWatchDatabase): ItemDao = database.itemDao()

    @Provides
    fun providesCategoryDao(database: ExpiryWatchDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun providesLocationDao(database: ExpiryWatchDatabase): LocationDao = database.locationDao()

    @Provides
    fun providesProductDao(database: ExpiryWatchDatabase): ProductDao = database.productDao()
}
