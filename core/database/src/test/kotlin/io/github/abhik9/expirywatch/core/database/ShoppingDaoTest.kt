package io.github.abhik9.expirywatch.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.database.dao.ShoppingListCount
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingHistoryEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingItemEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingListEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShoppingDaoTest {
    private lateinit var database: ExpiryWatchDatabase
    private val dao get() = database.shoppingDao()
    private val now = Instant.parse("2026-03-10T12:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ExpiryWatchDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    private fun item(listId: Long, name: String, categoryId: Long? = null, locationId: Long? = null) =
        ShoppingItemEntity(
            listId = listId,
            name = name,
            brand = null,
            barcode = null,
            categoryId = categoryId,
            locationId = locationId,
            quantity = 2.0,
            unit = QuantityUnit.LITERS,
            note = "lactose-free",
            checkedAt = null,
            createdAt = now,
        )

    @Test
    fun itemsComeWithTheirCategoryAndLocation() = runTest {
        val dairy = database.categoryDao().insert(CategoryEntity(name = "Dairy", emoji = "🧀"))
        val fridge = database.locationDao().insert(LocationEntity(name = "Fridge", emoji = "🧊"))
        val list = dao.insertList(ShoppingListEntity(name = "Groceries", createdAt = now))
        dao.insertItem(item(list, "Milk", categoryId = dairy, locationId = fridge))

        val milk = dao.observeItems(list).first().single().asExternalModel()

        assertEquals("Milk", milk.name)
        assertEquals("Dairy", milk.category?.name)
        assertEquals("Fridge", milk.location?.name)
        assertEquals(2.0, milk.quantity)
        assertEquals(QuantityUnit.LITERS, milk.unit)
        assertEquals("lactose-free", milk.note)
        assertEquals(false, milk.isChecked)
    }

    @Test
    fun deletingAListDeletesWhatsOnIt() = runTest {
        val groceries = dao.insertList(ShoppingListEntity(name = "Groceries", createdAt = now))
        val pharmacy = dao.insertList(ShoppingListEntity(name = "Pharmacy", createdAt = now))
        dao.insertItem(item(groceries, "Milk"))
        dao.insertItem(item(pharmacy, "Plasters"))

        dao.deleteList(groceries)

        assertEquals(listOf("Pharmacy"), dao.getLists().map { it.name })
        assertTrue(dao.getItems(groceries).isEmpty())
        assertEquals(listOf("Plasters"), dao.getItems(pharmacy).map { it.item.name })
    }

    @Test
    fun countsWhatIsLeftToBuyOnEachList() = runTest {
        val groceries = dao.insertList(ShoppingListEntity(name = "Groceries", createdAt = now))
        val pharmacy = dao.insertList(ShoppingListEntity(name = "Pharmacy", createdAt = now))
        dao.insertList(ShoppingListEntity(name = "Hardware", createdAt = now))
        dao.insertItem(item(groceries, "Milk"))
        dao.insertItem(item(groceries, "Bread"))
        dao.insertItem(item(pharmacy, "Plasters").copy(checkedAt = now))

        assertEquals(
            listOf(ShoppingListCount(groceries, 2)),
            dao.observeToBuyCounts().first(),
        )
        assertEquals(setOf("Milk", "Bread", "Plasters"), dao.observeItemNames().first().toSet())
    }

    @Test
    fun deletedCategoriesAndLocationsAreClearedFromItemsAndHistory() = runTest {
        val dairy = database.categoryDao().insert(CategoryEntity(name = "Dairy", emoji = "🧀"))
        val fridge = database.locationDao().insert(LocationEntity(name = "Fridge", emoji = "🧊"))
        val list = dao.insertList(ShoppingListEntity(name = "Groceries", createdAt = now))
        dao.insertItem(item(list, "Milk", categoryId = dairy, locationId = fridge))
        dao.upsertHistory(ShoppingHistoryEntity("milk", "Milk", dairy, timesAdded = 3, lastAddedAt = now))

        database.categoryDao().delete(dairy)
        database.locationDao().delete(fridge)

        val milk = dao.getItems(list).single()
        assertNull(milk.category)
        assertNull(milk.location)
        assertNull(dao.findInHistory("milk")?.entry?.categoryId)
    }

    @Test
    fun itemsAreCheckedAndUncheckedTogether() = runTest {
        val list = dao.insertList(ShoppingListEntity(name = "Groceries", createdAt = now))
        val milk = dao.insertItem(item(list, "Milk"))
        val bread = dao.insertItem(item(list, "Bread"))
        val eggs = dao.insertItem(item(list, "Eggs"))

        suspend fun checked() = dao.getItems(list).filter { it.item.checkedAt != null }.map { it.item.id }.toSet()

        dao.setCheckedAt(listOf(milk, bread), now)
        assertEquals(setOf(milk, bread), checked())

        dao.setCheckedAt(listOf(bread), null)
        assertEquals(setOf(milk), checked())
        assertNull(dao.getItems(list).single { it.item.id == eggs }.item.checkedAt)
    }

    @Test
    fun removedItemsCanBePutBackWithTheirIds() = runTest {
        val list = dao.insertList(ShoppingListEntity(name = "Groceries", createdAt = now))
        val milk = dao.insertItem(item(list, "Milk"))
        val removed = dao.getItems(list).map { it.item }

        dao.deleteItems(listOf(milk))
        assertTrue(dao.getItems(list).isEmpty())

        dao.insertOrReplaceItems(removed)
        assertEquals(removed, dao.getItems(list).map { it.item })
    }

    @Test
    fun historyIsSortedByHowOftenAndRecentlyThingsWereAdded() = runTest {
        fun entry(name: String, timesAdded: Int, lastAddedAt: Instant) =
            ShoppingHistoryEntity(name.lowercase(), name, null, timesAdded, lastAddedAt)
        dao.upsertHistory(entry("Bread", timesAdded = 1, lastAddedAt = now))
        dao.upsertHistory(entry("Milk", timesAdded = 5, lastAddedAt = now.minusSeconds(60)))
        dao.upsertHistory(entry("Eggs", timesAdded = 1, lastAddedAt = now.plusSeconds(60)))

        assertEquals(listOf("Milk", "Eggs", "Bread"), dao.observeHistory().first().map { it.entry.name })
    }
}
