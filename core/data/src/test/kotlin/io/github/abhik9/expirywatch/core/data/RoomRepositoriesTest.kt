package io.github.abhik9.expirywatch.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.data.repository.RoomBackupRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomCategoryRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomItemRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomProductHistoryRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomStorageLocationRepository
import io.github.abhik9.expirywatch.core.database.ExpiryWatchDatabase
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomRepositoriesTest {
    private val databases = mutableListOf<ExpiryWatchDatabase>()

    private fun newDatabase(): ExpiryWatchDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        ExpiryWatchDatabase::class.java,
    ).allowMainThreadQueries().build().also(databases::add)

    @After
    fun tearDown() = databases.forEach { it.close() }

    @Test
    fun itemsAreInsertedThenUpdated() = runTest {
        val database = newDatabase()
        val items = RoomItemRepository(database.itemDao())

        val id = items.upsert(TestData.item(name = "Milk", category = null, location = null))
        items.upsert(items.observeItem(id).first()!!.copy(name = "Oat milk"))

        assertEquals(listOf("Oat milk"), items.getActiveItems().map { it.name })
    }

    @Test
    fun categoriesAreSortedByName() = runTest {
        val categories = RoomCategoryRepository(newDatabase().categoryDao())
        categories.upsert(Category(name = "Snacks", emoji = "🍫"))
        categories.upsert(Category(name = "bakery", emoji = "🍞"))
        categories.upsert(Category(name = "Dairy", emoji = "🧀"))

        assertEquals(listOf("bakery", "Dairy", "Snacks"), categories.observeCategories().first().map { it.name })
    }

    @Test
    fun backupRestoresEverythingIntoAnotherDatabase() = runTest {
        val source = newDatabase()
        val categoryId = RoomCategoryRepository(source.categoryDao()).upsert(Category(name = "Dairy", emoji = "🧀"))
        val locationId = RoomStorageLocationRepository(source.locationDao())
            .upsert(StorageLocation(name = "Fridge", emoji = "🧊"))
        val dairy = Category(categoryId, "Dairy", "🧀")
        val fridge = StorageLocation(locationId, "Fridge", "🧊")
        val sourceItems = RoomItemRepository(source.itemDao())
        sourceItems.upsert(TestData.item(name = "Milk", category = dairy, location = fridge).copy(barcode = "123"))
        val finishedId = sourceItems.upsert(TestData.item(name = "Spinach", category = null, location = null))
        sourceItems.updateStatus(finishedId, ItemStatus.WASTED, TestTime.today)
        RoomProductHistoryRepository(source.productDao()).remember(
            ProductInfo(barcode = "123", name = "Milk", categoryId = categoryId, source = ProductSource.HISTORY),
        )

        val snapshot = RoomBackupRepository(source).createSnapshot()
        val target = newDatabase()
        // Pre-existing data in the target is replaced, not merged.
        RoomCategoryRepository(target.categoryDao()).upsert(Category(name = "Stale", emoji = "❓"))
        RoomBackupRepository(target).restore(snapshot)

        assertEquals(snapshot, RoomBackupRepository(target).createSnapshot())
        assertEquals(
            listOf("Dairy"),
            RoomCategoryRepository(target.categoryDao()).observeCategories().first().map { it.name },
        )
        val restoredMilk = RoomItemRepository(target.itemDao()).getActiveItems().single()
        assertEquals(dairy, restoredMilk.category)
        assertEquals(fridge, restoredMilk.location)
        assertEquals("Milk", RoomProductHistoryRepository(target.productDao()).find("123")?.name)
    }
}
