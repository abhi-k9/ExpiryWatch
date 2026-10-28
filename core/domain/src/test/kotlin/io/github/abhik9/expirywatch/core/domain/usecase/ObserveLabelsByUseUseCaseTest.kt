package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeCustomGroupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ObserveLabelsByUseUseCaseTest {
    private fun at(minutes: Long): Instant = TestTime.now.plusSeconds(minutes * 60)

    @Test
    fun theTwoMostOftenUsedLeadThenTheThreeMostRecentThenTheRestByName() {
        val names = listOf("Attic", "Bathroom", "Cellar", "Drawer", "Freezer", "Fridge", "Garage", "Pantry")
        val labels = names.mapIndexed { index, name -> (index + 1L) to name }
        val id = labels.toMap().entries.associate { (k, v) -> v to k }
        val uses = listOf(
            // Fridge 3 times, Pantry twice, Freezer twice but longer ago.
            LabelUse(id.getValue("Fridge"), at(1)),
            LabelUse(id.getValue("Fridge"), at(2)),
            LabelUse(id.getValue("Fridge"), at(3)),
            LabelUse(id.getValue("Pantry"), at(10)),
            LabelUse(id.getValue("Pantry"), at(11)),
            LabelUse(id.getValue("Freezer"), at(4)),
            LabelUse(id.getValue("Freezer"), at(5)),
            // Used once each, most recently Cellar, then Attic, then Garage, then Drawer.
            LabelUse(id.getValue("Cellar"), at(30)),
            LabelUse(id.getValue("Attic"), at(20)),
            LabelUse(id.getValue("Garage"), at(15)),
            LabelUse(id.getValue("Drawer"), at(12)),
        )

        val ordered = orderByUse(labels, uses) { it.first }.map { it.second }

        assertEquals(
            listOf("Fridge", "Pantry", "Cellar", "Attic", "Garage", "Bathroom", "Drawer", "Freezer"),
            ordered,
        )
    }

    @Test
    fun withoutUsesTheOrderIsByName() {
        val labels = listOf(1L to "Attic", 2L to "Cellar", 3L to "Fridge")

        assertEquals(labels, orderByUse(labels, emptyList()) { it.first })
    }

    @Test
    fun categoryShortcutsAreRecentDistinctAndExcludeUnusedOrRemovedCategories() = runTest {
        val categories = (1L..5L).map { Category(id = it, name = "Category $it", emoji = "📦") }
        val items = FakeItemRepository(
            listOf(
                TestData.item(id = 1, category = categories[0]).copy(updatedAt = at(1)),
                TestData.item(id = 2, category = categories[0]).copy(updatedAt = at(2)),
                TestData.item(id = 3, category = categories[1]).copy(updatedAt = at(3)),
                TestData.item(id = 4, category = categories[2]).copy(updatedAt = at(5)),
                TestData.item(id = 5, category = categories[2]).copy(updatedAt = at(6)),
                TestData.item(id = 6, category = categories[3]).copy(updatedAt = at(4)),
                TestData.item(id = 7, category = Category(id = 99, name = "Removed", emoji = "📦"))
                    .copy(updatedAt = at(7)),
            ),
        )
        val useCase = ObserveLabelsByUseUseCase(
            FakeCategoryRepository(categories),
            FakeStorageLocationRepository(),
            FakeCustomGroupRepository(),
            items,
            TestTime.clock,
        )

        assertEquals(listOf(3L, 4L, 2L), useCase().first().recentCategories.map { it.id })
        items.setItems(emptyList())
        assertEquals(emptyList(), useCase().first().recentCategories)
    }

    @Test
    fun countsUsesOnItemsInStockAndRecentlyFinished() = runTest {
        val freezer = StorageLocation(id = 3, name = "Freezer", emoji = "❄️")
        val camping = CustomGroup(id = 1, name = "Camping kit", emoji = "⛺")
        val items = FakeItemRepository(
            listOf(
                TestData.item(id = 1, location = TestData.pantry).copy(updatedAt = at(1)),
                TestData.item(id = 2, location = TestData.pantry, category = TestData.produce).copy(updatedAt = at(2)),
                TestData.item(id = 3, location = freezer).copy(
                    status = ItemStatus.CONSUMED,
                    finishedDate = TestTime.today.minusDays(3),
                    updatedAt = at(3),
                    customGroup = camping,
                ),
            ),
        )
        val useCase = ObserveLabelsByUseUseCase(
            FakeCategoryRepository(listOf(TestData.dairy, TestData.produce)),
            FakeStorageLocationRepository(listOf(TestData.fridge, TestData.pantry, freezer)),
            FakeCustomGroupRepository(listOf(CustomGroup(id = 2, name = "Baby", emoji = "🍼"), camping)),
            items,
            TestTime.clock,
        )

        val labels = useCase().first()

        assertEquals(listOf("Pantry", "Freezer", "Fridge"), labels.locations.map { it.name })
        assertEquals(listOf("Dairy", "Produce"), labels.categories.map { it.name })
        assertEquals(listOf("Camping kit", "Baby"), labels.groups.map { it.name })
    }
}
