package io.github.abhik9.expirywatch.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.abhik9.expirywatch.core.database.dao.CategoryDao
import io.github.abhik9.expirywatch.core.database.dao.ItemDao
import io.github.abhik9.expirywatch.core.database.dao.LocationDao
import io.github.abhik9.expirywatch.core.database.dao.ProductDao
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.ItemEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import io.github.abhik9.expirywatch.core.database.model.ProductEntity
import io.github.abhik9.expirywatch.core.database.util.Converters

/**
 * The app's single database. When changing the schema, bump [version] and add an
 * [androidx.room.AutoMigration] (or a manual migration) so users keep their data.
 */
@Database(
    entities = [
        CategoryEntity::class,
        LocationEntity::class,
        ItemEntity::class,
        ProductEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ExpiryWatchDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun categoryDao(): CategoryDao

    abstract fun locationDao(): LocationDao

    abstract fun productDao(): ProductDao

    companion object {
        const val NAME = "expirywatch.db"
    }
}
