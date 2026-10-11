package io.github.abhik9.expirywatch.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.database.model.ShoppingItemEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingListEntity
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Opens databases written by earlier versions of the app, as users have them on their devices. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = File(context.cacheDir, "migration-test.db")
    private var database: ExpiryWatchDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        SQLiteDatabase.deleteDatabase(file)
    }

    /** Writes a version 1 database, as described by schemas/…/1.json, with some data in it. */
    private fun createVersion1() {
        file.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        listOf(
            "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `emoji` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `emoji` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `brand` TEXT, `barcode` TEXT, `category_id` INTEGER, `location_id` INTEGER, " +
                "`quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `expiry_date` INTEGER NOT NULL, " +
                "`opened_date` INTEGER, `use_within_days` INTEGER, `notes` TEXT, `image_url` TEXT, " +
                "`status` TEXT NOT NULL, `finished_date` INTEGER, `created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
                "FOREIGN KEY(`location_id`) REFERENCES `locations`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_items_category_id` ON `items` (`category_id`)",
            "CREATE INDEX IF NOT EXISTS `index_items_location_id` ON `items` (`location_id`)",
            "CREATE INDEX IF NOT EXISTS `index_items_status_finished_date` ON `items` (`status`, `finished_date`)",
            "CREATE TABLE IF NOT EXISTS `products` (`barcode` TEXT NOT NULL, `name` TEXT NOT NULL, `brand` TEXT, " +
                "`image_url` TEXT, `category_id` INTEGER, `location_id` INTEGER, `quantity` REAL, `unit` TEXT, " +
                "`use_within_days` INTEGER, PRIMARY KEY(`barcode`), " +
                "FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
                "FOREIGN KEY(`location_id`) REFERENCES `locations`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE INDEX IF NOT EXISTS `index_products_category_id` ON `products` (`category_id`)",
            "CREATE INDEX IF NOT EXISTS `index_products_location_id` ON `products` (`location_id`)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                "VALUES(42, '1f0512fc91932c43c4f3f5dcc50089c7')",
            "INSERT INTO categories (id, name, emoji) VALUES (1, 'Dairy', '🧀')",
            "INSERT INTO items (id, name, category_id, quantity, unit, expiry_date, status, created_at, updated_at) " +
                "VALUES (1, 'Milk', 1, 1.0, 'LITERS', 20522, 'ACTIVE', 0, 0)",
        ).forEach(db::execSQL)
        db.version = 1
        db.close()
    }

    private fun open(): ExpiryWatchDatabase =
        Room.databaseBuilder(context, ExpiryWatchDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

    @Test
    fun version1KeepsItsItemsAndGainsShoppingLists() = runTest {
        createVersion1()

        val db = open()

        val milk = db.itemDao().observeActive().first().single()
        assertEquals("Milk", milk.item.name)
        assertEquals("Dairy", milk.category?.name)

        val now = Instant.parse("2026-03-10T12:00:00Z")
        val list = db.shoppingDao().insertList(ShoppingListEntity(name = "Groceries", createdAt = now))
        db.shoppingDao().insertItem(
            ShoppingItemEntity(
                listId = list,
                name = "Milk",
                brand = null,
                barcode = null,
                categoryId = 1,
                locationId = null,
                quantity = 1.0,
                unit = QuantityUnit.LITERS,
                note = null,
                checkedAt = null,
                createdAt = now,
            ),
        )
        assertEquals("Dairy", db.shoppingDao().getItems(list).single().category?.name)
    }
}
