package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemsOverviewTest {
    private val today = TestTime.today

    private val milk = TestData.item(id = 1, name = "Milk", expiresInDays = 1)
    private val yogurt = TestData.item(id = 2, name = "Yogurt", expiresInDays = -2)
    private val apples = TestData.item(id = 3, name = "Apples", expiresInDays = 10, category = TestData.produce)
    private val creme = TestData.item(id = 4, name = "Crème fraîche", expiresInDays = 3, location = TestData.pantry)

    private val all = listOf(milk, yogurt, apples, creme)

    private fun overview(
        query: ItemQuery = ItemQuery(),
        sortOrder: ItemSortOrder = ItemSortOrder.EXPIRY_SOONEST,
        items: List<Item> = all,
    ) = buildItemsOverview(items, query, sortOrder, today, expiringSoonDays = 3)

    @Test
    fun computesStatusAndDaysForEachItem() {
        val result = overview().items.associateBy { it.item.id }

        assertEquals(ExpiryStatus.EXPIRING_SOON, result.getValue(1).status)
        assertEquals(1, result.getValue(1).daysUntilExpiry)
        assertEquals(ExpiryStatus.EXPIRED, result.getValue(2).status)
        assertEquals(-2, result.getValue(2).daysUntilExpiry)
        assertEquals(ExpiryStatus.FRESH, result.getValue(3).status)
    }

    @Test
    fun sortsBySoonestExpiryByDefault() {
        assertEquals(listOf("Yogurt", "Milk", "Crème fraîche", "Apples"), overview().items.map { it.item.name })
    }

    @Test
    fun supportsOtherSortOrders() {
        assertEquals(
            listOf("Apples", "Crème fraîche", "Milk", "Yogurt"),
            overview(sortOrder = ItemSortOrder.EXPIRY_LATEST).items.map { it.item.name },
        )
        assertEquals(
            listOf("Apples", "Crème fraîche", "Milk", "Yogurt"),
            overview(sortOrder = ItemSortOrder.NAME).items.map { it.item.name },
        )

        val older = milk.copy(createdAt = Instant.EPOCH)
        val newer = apples.copy(createdAt = TestTime.now)
        assertEquals(
            listOf("Apples", "Milk"),
            overview(sortOrder = ItemSortOrder.RECENTLY_ADDED, items = listOf(older, newer)).items.map { it.item.name },
        )
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndMatchesAllTerms() {
        assertEquals(listOf("Crème fraîche"), overview(ItemQuery(searchText = "CREME")).items.map { it.item.name })
        assertEquals(
            listOf("Crème fraîche"),
            overview(ItemQuery(searchText = "fraiche creme")).items.map { it.item.name },
        )
        assertEquals(emptyList(), overview(ItemQuery(searchText = "creme milk")).items)
    }

    @Test
    fun searchMatchesCategoryAndLocationNames() {
        assertEquals(listOf("Apples"), overview(ItemQuery(searchText = "produce")).items.map { it.item.name })
        assertEquals(listOf("Crème fraîche"), overview(ItemQuery(searchText = "pantry")).items.map { it.item.name })
    }

    @Test
    fun filtersByCategoryLocationAndStatus() {
        assertEquals(listOf(3L), overview(ItemQuery(categoryId = TestData.produce.id)).items.map { it.item.id })
        assertEquals(listOf(4L), overview(ItemQuery(locationId = TestData.pantry.id)).items.map { it.item.id })
        assertEquals(listOf(2L), overview(ItemQuery(status = ExpiryStatus.EXPIRED)).items.map { it.item.id })
    }

    @Test
    fun statusCountsIgnoreTheStatusFilterButRespectOthers() {
        val result = overview(ItemQuery(status = ExpiryStatus.EXPIRED, categoryId = TestData.dairy.id))

        assertEquals(
            mapOf(ExpiryStatus.EXPIRED to 1, ExpiryStatus.EXPIRING_SOON to 2, ExpiryStatus.FRESH to 0),
            result.statusCounts,
        )
        assertEquals(4, result.totalActiveCount)
    }
}
