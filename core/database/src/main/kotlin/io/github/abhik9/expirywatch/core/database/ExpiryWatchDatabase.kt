package io.github.abhik9.expirywatch.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.abhik9.expirywatch.core.database.dao.CategoryDao
import io.github.abhik9.expirywatch.core.database.dao.ItemDao
import io.github.abhik9.expirywatch.core.database.dao.LocationDao
import io.github.abhik9.expirywatch.core.database.dao.ProductDao
import io.github.abhik9.expirywatch.core.database.dao.ShoppingDao
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.ItemEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import io.github.abhik9.expirywatch.core.database.model.ProductEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingHistoryEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingItemEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingListEntity
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
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        ShoppingHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // Adds the shopping lists.
        AutoMigration(from = 1, to = 2),
        // Adds snoozing an item's reminders.
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class ExpiryWatchDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun categoryDao(): CategoryDao

    abstract fun locationDao(): LocationDao

    abstract fun productDao(): ProductDao

    abstract fun shoppingDao(): ShoppingDao

    companion object {
        const val NAME = "expirywatch.db"
    }
}
