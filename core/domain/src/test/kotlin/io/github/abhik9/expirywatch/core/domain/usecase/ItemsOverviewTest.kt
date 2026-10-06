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

    private fun ItemsOverview.layout(): List<Pair<ExpiryStatus?, List<List<Long>>>> =
        sections.map { section -> section.status to section.groups.map { group -> group.entries.map { it.item.id } } }

    @Test
    fun sectionsFollowTheStatusesInSortOrder() {
        assertEquals(
            listOf(
                ExpiryStatus.EXPIRED to listOf(listOf(2L)),
                ExpiryStatus.EXPIRING_SOON to listOf(listOf(1L), listOf(4L)),
                ExpiryStatus.FRESH to listOf(listOf(3L)),
            ),
            overview().layout(),
        )
    }

    @Test
    fun itemsOfTheSameProductAreGroupedSoonestFirst() {
        val moreMilk = TestData.item(id = 5, name = " MILK ", expiresInDays = 3)
        val evenMoreMilk = TestData.item(id = 6, name = "milk", expiresInDays = 2)

        val result = overview(items = all + moreMilk + evenMoreMilk)

        val soon = result.sections.single { it.status == ExpiryStatus.EXPIRING_SOON }
        assertEquals(
            listOf(listOf(1L, 6L, 5L), listOf(4L)),
            soon.groups.map { group -> group.entries.map { it.item.id } },
        )
        assertEquals(milk, soon.groups.first().next.item)
        assertEquals(4, soon.itemCount)
    }

    @Test
    fun aProductIsGroupedWithinEachStatusItHasItemsIn() {
        val freshMilk = TestData.item(id = 5, name = "Milk", expiresInDays = 20)
        val staleMilk = TestData.item(id = 6, name = "Milk", expiresInDays = -1)

        val layout = overview(items = listOf(milk, freshMilk, staleMilk)).layout()

        assertEquals(
            listOf(
                ExpiryStatus.EXPIRED to listOf(listOf(6L)),
                ExpiryStatus.EXPIRING_SOON to listOf(listOf(1L)),
                ExpiryStatus.FRESH to listOf(listOf(5L)),
            ),
            layout,
        )
    }

    @Test
    fun withoutStatusSectionsAProductIsOneGroupPlacedByItsFirstItem() {
        val freshMilk = TestData.item(id = 5, name = "Milk", expiresInDays = 20)
        val staleMilk = TestData.item(id = 6, name = "Milk", expiresInDays = -1)
        val items = listOf(milk, apples, freshMilk, staleMilk)

        assertEquals(
            listOf(null to listOf(listOf(3L), listOf(6L, 1L, 5L))),
            overview(sortOrder = ItemSortOrder.NAME, items = items).layout(),
        )
        assertEquals(
            listOf(null to listOf(listOf(6L, 1L, 5L))),
            overview(query = ItemQuery(searchText = "milk"), sortOrder = ItemSortOrder.NAME, items = items).layout(),
        )
        // Filtering by status shows only that status's items, still grouped.
        assertEquals(
            listOf(null to listOf(listOf(1L))),
            overview(query = ItemQuery(status = ExpiryStatus.EXPIRING_SOON), items = items).layout(),
        )
    }

    @Test
    fun groupKeysAreUniqueAcrossSections() {
        val freshMilk = TestData.item(id = 5, name = "Milk", expiresInDays = 20)

        val keys = overview(
            items = listOf(milk, freshMilk),
        ).sections.flatMap { section -> section.groups.map { it.key } }

        assertEquals(2, keys.toSet().size)
    }
}
