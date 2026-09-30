package io.github.abhik9.expirywatch.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.ItemEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemDaoTest {
    private lateinit var database: ExpiryWatchDatabase
    private val today = LocalDate.of(2026, 3, 10)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ExpiryWatchDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    private fun item(
        name: String,
        categoryId: Long? = null,
        locationId: Long? = null,
        status: ItemStatus = ItemStatus.ACTIVE,
        finishedDate: LocalDate? = null,
    ) = ItemEntity(
        name = name,
        brand = null,
        barcode = null,
        categoryId = categoryId,
        locationId = locationId,
        quantity = 1.0,
        unit = QuantityUnit.PIECES,
        expiryDate = today.plusDays(3),
        openedDate = null,
        useWithinDaysAfterOpening = null,
        notes = null,
        imageUrl = null,
        status = status,
        finishedDate = finishedDate,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun activeItemsComeWithTheirCategoryAndLocation() = runTest {
        val dairy = database.categoryDao().insert(CategoryEntity(name = "Dairy", emoji = "🧀"))
        val fridge = database.locationDao().insert(LocationEntity(name = "Fridge", emoji = "🧊"))
        database.itemDao().insert(item("Milk", categoryId = dairy, locationId = fridge))
        database.itemDao().insert(item("Old", status = ItemStatus.CONSUMED, finishedDate = today))

        val milk = database.itemDao().observeActive().first().single().asExternalModel()

        assertEquals("Milk", milk.name)
        assertEquals("Dairy", milk.category?.name)
        assertEquals("Fridge", milk.location?.name)
        assertEquals(today.plusDays(3), milk.expiryDate)
    }

    @Test
    fun deletingACategoryKeepsItsItems() = runTest {
        val dairy = database.categoryDao().insert(CategoryEntity(name = "Dairy", emoji = "🧀"))
        val id = database.itemDao().insert(item("Milk", categoryId = dairy))

        database.categoryDao().delete(dairy)

        val milk = database.itemDao().observe(id).first()
        assertEquals("Milk", milk?.item?.name)
        assertNull(milk?.item?.categoryId)
        assertNull(milk?.category)
    }

    @Test
    fun finishedItemsAreFilteredByDate() = runTest {
        database.itemDao().insert(item("Recent", status = ItemStatus.WASTED, finishedDate = today))
        database.itemDao().insert(item("Old", status = ItemStatus.CONSUMED, finishedDate = today.minusYears(2)))
        database.itemDao().insert(item("Active"))

        val finished = database.itemDao().observeFinished(since = today.minusMonths(1)).first()

        assertEquals(listOf("Recent"), finished.map { it.item.name })
    }

    @Test
    fun statusUpdatesRecordTheFinishDate() = runTest {
        val id = database.itemDao().insert(item("Milk"))

        database.itemDao().updateStatus(id, ItemStatus.CONSUMED, today)

        assertEquals(emptyList(), database.itemDao().getActive())
        val updated = database.itemDao().getAll().single()
        assertEquals(ItemStatus.CONSUMED, updated.status)
        assertEquals(today, updated.finishedDate)
    }
}
