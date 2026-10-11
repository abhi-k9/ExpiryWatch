package io.github.abhik9.expirywatch.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.data.repository.RoomBackupRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomCategoryRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomItemRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomProductHistoryRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomShoppingRepository
import io.github.abhik9.expirywatch.core.data.repository.RoomStorageLocationRepository
import io.github.abhik9.expirywatch.core.database.ExpiryWatchDatabase
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
        val items = RoomItemRepository(database, EventLog.NONE)

        val id = items.upsert(TestData.item(name = "Milk", category = null, location = null))
        items.upsert(items.observeItem(id).first()!!.copy(name = "Oat milk"))

        assertEquals(listOf("Oat milk"), items.getActiveItems().map { it.name })
    }

    @Test
    fun severalItemsAreSavedTogetherOrNotAtAll() = runTest {
        val items = RoomItemRepository(newDatabase(), EventLog.NONE)

        val ids = items.upsertAll(
            listOf(
                TestData.item(name = "Milk", expiresInDays = 2, category = null, location = null),
                TestData.item(name = "Milk", expiresInDays = 9, category = null, location = null),
            ),
        )
        assertEquals(2, ids.toSet().size)

        // The second item points at a category that doesn't exist, so the first one isn't kept either.
        val missingCategory = Category(id = 999, name = "Gone", emoji = "❓")
        assertFailsWith<Exception> {
            items.upsertAll(
                listOf(
                    TestData.item(name = "Bread", category = null, location = null),
                    TestData.item(name = "Cheese", category = missingCategory, location = null),
                ),
            )
        }
        assertEquals(listOf("Milk", "Milk"), items.getActiveItems().map { it.name })
    }

    @Test
    fun shoppingHistoryCountsAdditionsAndKeepsTheCategory() = runTest {
        val database = newDatabase()
        val dairyId = RoomCategoryRepository(database.categoryDao()).upsert(Category(name = "Dairy", emoji = "🧀"))
        val dairy = Category(dairyId, "Dairy", "🧀")
        val shopping = RoomShoppingRepository(database, EventLog.NONE)

        shopping.rememberAdded("Milk", dairy, TestTime.now)
        shopping.rememberAdded(" oat MILK ", null, TestTime.now)
        shopping.rememberAdded(" MILK ", null, TestTime.now.plusSeconds(60))

        val milk = shopping.findInHistory("milk")!!
        assertEquals("MILK", milk.name)
        assertEquals(dairy, milk.category)
        assertEquals(2, milk.timesAdded)
        assertEquals(TestTime.now.plusSeconds(60), milk.lastAddedAt)
        assertEquals(listOf("MILK", "oat MILK"), shopping.observeHistory().first().map { it.name })
    }

    @Test
    fun shoppingHistoryLearnsANewNameOrCategoryWithoutCountingIt() = runTest {
        val database = newDatabase()
        val dairyId = RoomCategoryRepository(database.categoryDao()).upsert(Category(name = "Dairy", emoji = "🧀"))
        val dairy = Category(dairyId, "Dairy", "🧀")
        val shopping = RoomShoppingRepository(database, EventLog.NONE)
        shopping.rememberAdded("milk", null, TestTime.now)

        shopping.rememberProduct("Milk", dairy, TestTime.now.plusSeconds(60))
        shopping.rememberProduct("Oat milk", null, TestTime.now.plusSeconds(60))

        val milk = shopping.findInHistory("milk")!!
        assertEquals("Milk", milk.name)
        assertEquals(dairy, milk.category)
        assertEquals(1, milk.timesAdded)
        assertEquals(TestTime.now, milk.lastAddedAt)
        assertEquals(1, shopping.findInHistory("oat milk")?.timesAdded)
    }

    @Test
    fun itemsPutBackOnADeletedListStayDeleted() = runTest {
        val shopping = RoomShoppingRepository(newDatabase(), EventLog.NONE)
        val groceries = shopping.createList("Groceries", TestTime.now)
        val pharmacy = shopping.createList("Pharmacy", TestTime.now)
        shopping.upsertItem(ShoppingItem(listId = groceries, name = "Milk", createdAt = TestTime.now))
        shopping.upsertItem(ShoppingItem(listId = pharmacy, name = "Plasters", createdAt = TestTime.now))
        val removed = shopping.getItems(groceries) + shopping.getItems(pharmacy)
        shopping.deleteItems(removed.map { it.id })

        // Undoing the removal after the list itself was deleted.
        shopping.deleteList(groceries)
        shopping.restoreItems(removed)

        assertEquals(listOf("Plasters"), shopping.getItems(pharmacy).map { it.name })
        assertEquals(emptyList(), shopping.getItems(groceries))
    }

    @Test
    fun shoppingChangesAreRecordedByIdOnly() = runTest {
        val log = RecordingEventLog()
        val shopping = RoomShoppingRepository(newDatabase(), log)

        val list = shopping.createList("Groceries", TestTime.now)
        val milk = shopping.upsertItem(ShoppingItem(listId = list, name = "Milk", createdAt = TestTime.now))
        shopping.setChecked(listOf(milk), TestTime.now)
        shopping.deleteItems(listOf(milk))

        assertEquals(
            listOf(
                "shopping: created list #$list",
                "shopping: added #$milk to list #$list",
                "shopping: checked #$milk",
                "shopping: removed #$milk",
            ),
            log.messages,
        )
    }

    @Test
    fun itemChangesAreRecordedByIdOnly() = runTest {
        val log = RecordingEventLog()
        val items = RoomItemRepository(newDatabase(), log)

        val id = items.upsert(TestData.item(name = "Milk", category = null, location = null))
        items.updateStatus(id, ItemStatus.CONSUMED, TestTime.today)
        items.delete(id)

        assertEquals(
            listOf("items: added #$id, expires 2026-03-15", "items: #$id is now CONSUMED", "items: deleted #$id"),
            log.messages,
        )
    }

    @Test
    fun diagnosticsCountWhatIsStored() = runTest {
        val database = newDatabase()
        val items = RoomItemRepository(database, EventLog.NONE)
        items.upsert(TestData.item(name = "Milk", category = null, location = null))
        val finished = items.upsert(TestData.item(name = "Bread", category = null, location = null))
        items.updateStatus(finished, ItemStatus.WASTED, TestTime.today)
        val section = DataDiagnosticsSection(
            database.itemDao(),
            database.categoryDao(),
            database.locationDao(),
            database.productDao(),
            database.shoppingDao(),
            FakeUserSettingsRepository(),
        )

        val lines = section.describe()

        assertEquals("Items: 1 active, 0 used up, 1 thrown away", lines[0])
        assertEquals("Categories: 0, locations: 0, remembered products: 0", lines[1])
        assertEquals("Shopping lists: 0, with 0 items", lines[2])
        assertFalse(lines.any { "Milk" in it || "Bread" in it })
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
        val sourceItems = RoomItemRepository(source, EventLog.NONE)
        sourceItems.upsert(TestData.item(name = "Milk", category = dairy, location = fridge).copy(barcode = "123"))
        val finishedId = sourceItems.upsert(TestData.item(name = "Spinach", category = null, location = null))
        sourceItems.updateStatus(finishedId, ItemStatus.WASTED, TestTime.today)
        RoomProductHistoryRepository(source.productDao()).remember(
            ProductInfo(barcode = "123", name = "Milk", categoryId = categoryId, source = ProductSource.HISTORY),
        )
        val sourceShopping = RoomShoppingRepository(source, EventLog.NONE)
        val groceries = sourceShopping.createList("Groceries", TestTime.now)
        sourceShopping.upsertItem(
            ShoppingItem(
                listId = groceries,
                name = "Butter",
                category = dairy,
                location = fridge,
                note = "unsalted",
                createdAt = TestTime.now,
            ),
        )
        sourceShopping.rememberAdded("Butter", dairy, TestTime.now)

        val snapshot = RoomBackupRepository(source).createSnapshot()
        val target = newDatabase()
        // Pre-existing data in the target is replaced, not merged.
        RoomCategoryRepository(target.categoryDao()).upsert(Category(name = "Stale", emoji = "❓"))
        RoomShoppingRepository(target, EventLog.NONE).createList("Stale list", TestTime.now)
        RoomBackupRepository(target).restore(snapshot)

        assertEquals(snapshot, RoomBackupRepository(target).createSnapshot())
        assertEquals(
            listOf("Dairy"),
            RoomCategoryRepository(target.categoryDao()).observeCategories().first().map { it.name },
        )
        val targetShopping = RoomShoppingRepository(target, EventLog.NONE)
        assertEquals(listOf("Groceries"), targetShopping.getLists().map { it.name })
        val butter = targetShopping.getItems(groceries).single()
        assertEquals(Triple("Butter", dairy, fridge), Triple(butter.name, butter.category, butter.location))
        assertEquals(dairy, targetShopping.findInHistory("butter")?.category)
        val restoredMilk = RoomItemRepository(target, EventLog.NONE).getActiveItems().single()
        assertEquals(dairy, restoredMilk.category)
        assertEquals(fridge, restoredMilk.location)
        assertEquals("Milk", RoomProductHistoryRepository(target.productDao()).find("123")?.name)
    }

    @Test
    fun restoringABackupFromBeforeShoppingListsKeepsTheCurrentOnes() = runTest {
        val database = newDatabase()
        val categories = RoomCategoryRepository(database.categoryDao())
        val oldDairy = Category(categories.upsert(Category(name = "Dairy", emoji = "🧀")), "Dairy", "🧀")
        val snacks = Category(categories.upsert(Category(name = "Snacks", emoji = "🍫")), "Snacks", "🍫")
        val shopping = RoomShoppingRepository(database, EventLog.NONE)
        val list = shopping.createList("Groceries", TestTime.now)
        shopping.upsertItem(ShoppingItem(listId = list, name = "Milk", category = oldDairy, createdAt = TestTime.now))
        shopping.upsertItem(ShoppingItem(listId = list, name = "Crisps", category = snacks, createdAt = TestTime.now))
        shopping.rememberAdded("Milk", oldDairy, TestTime.now)

        // The backup has its own Dairy, under another id, and no Snacks.
        val dairy = Category(id = 40, name = "Dairy", emoji = "🥛")
        RoomBackupRepository(database).restore(
            BackupSnapshot(
                categories = listOf(dairy),
                locations = emptyList(),
                items = emptyList(),
                products = emptyList(),
            ),
        )

        assertEquals(listOf("Groceries"), shopping.getLists().map { it.name })
        assertEquals(
            mapOf("Milk" to dairy, "Crisps" to null),
            shopping.getItems(list).associate { it.name to it.category },
        )
        assertEquals(dairy, shopping.findInHistory("milk")?.category)
    }
}
