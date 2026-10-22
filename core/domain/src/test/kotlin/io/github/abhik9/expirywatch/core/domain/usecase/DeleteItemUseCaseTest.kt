package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class DeleteItemUseCaseTest {
    @Test
    fun aDeletedItemIsOfferedOnceForUndoingAndComesBackAsItWas() = runTest {
        val milk = TestData.item(id = 1, name = "Milk")
        val items = FakeItemRepository(listOf(milk))
        val recentlyDeleted = RecentlyDeletedItems()
        val deleteItem = DeleteItemUseCase(items, recentlyDeleted)

        deleteItem(milk)
        assertNull(items.observeItem(1).first())

        assertEquals(milk, recentlyDeleted.take())
        assertNull(recentlyDeleted.take())
        deleteItem.undo(milk)
        assertEquals(milk, items.observeItem(1).first())
    }
}
