package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.repository.FakeCustomGroupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class MoveItemsToGroupUseCaseTest {
    private val camping = CustomGroup(id = 1, name = "Camping kit", emoji = "⛺")
    private val baby = CustomGroup(id = 2, name = "Baby", emoji = "🍼")
    private val groups = FakeCustomGroupRepository(listOf(camping, baby))
    private val beans = TestData.item(id = 1, name = "Beans").copy(customGroup = camping)
    private val formula = TestData.item(id = 2, name = "Formula").copy(customGroup = baby)
    private val milk = TestData.item(id = 3, name = "Milk")
    private val items = FakeItemRepository(listOf(beans, formula, milk), groups)
    private val moveToGroup = MoveItemsToGroupUseCase(items)

    private fun groupNames() = items.currentItems.associate { it.name to it.customGroup?.name }

    @Test
    fun movesItemsAndPutsEachBackWhereItWas() = runTest {
        moveToGroup(listOf(beans, formula, milk), baby.id)
        assertEquals(mapOf("Beans" to "Baby", "Formula" to "Baby", "Milk" to "Baby"), groupNames())

        moveToGroup.restore(listOf(beans, formula, milk))
        assertEquals(mapOf("Beans" to "Camping kit", "Formula" to "Baby", "Milk" to null), groupNames())
    }

    @Test
    fun takesItemsOutOfTheirGroup() = runTest {
        moveToGroup(listOf(beans), groupId = null)

        assertEquals(null, groupNames()["Beans"])
    }
}
