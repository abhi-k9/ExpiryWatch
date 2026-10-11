package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class RestockSuggestionsTest {
    private val items = FakeItemRepository()
    private val shopping = FakeShoppingRepository()
    private val settings = FakeUserSettingsRepository()
    private val observeSuggestions = ObserveRestockSuggestionsUseCase(
        items,
        shopping,
        settings,
        ObserveTodayUseCase(TestTime.clock),
        TestTime.clock,
    )
    private val today = TestTime.today

    private fun finished(id: Long, name: String, status: ItemStatus, daysAgo: Long): Item =
        TestData.item(id = id, name = name).copy(status = status, finishedDate = today.minusDays(daysAgo))

    private suspend fun suggestions() = observeSuggestions().first()

    @Test
    fun suggestsWhatExpiredOrRanOutRecentlyMostRecentFirst() = runTest {
        items.setItems(
            listOf(
                TestData.item(id = 1, name = "Milk", expiresInDays = -2),
                finished(2, "Bread", ItemStatus.CONSUMED, daysAgo = 1),
                finished(3, "Spinach", ItemStatus.WASTED, daysAgo = 0),
                finished(4, "Old jam", ItemStatus.CONSUMED, daysAgo = 30),
            ),
        )

        assertEquals(
            listOf(
                RestockSuggestion("Spinach", TestData.dairy, RestockReason.THROWN_AWAY, today, daysAgo = 0),
                RestockSuggestion("Bread", TestData.dairy, RestockReason.USED_UP, today.minusDays(1), daysAgo = 1),
                RestockSuggestion("Milk", TestData.dairy, RestockReason.EXPIRED, today.minusDays(2), daysAgo = 2),
            ),
            suggestions(),
        )
    }

    @Test
    fun leavesOutWhatIsStillGoodSomewhere() = runTest {
        items.setItems(
            listOf(
                TestData.item(id = 1, name = "Milk", expiresInDays = -2),
                TestData.item(id = 2, name = "milk", expiresInDays = 4),
                finished(3, "Bread", ItemStatus.CONSUMED, daysAgo = 1),
                TestData.item(id = 4, name = "Bread", expiresInDays = 2),
                // Expiring today can still be used.
                TestData.item(id = 5, name = "Eggs", expiresInDays = 0),
            ),
        )

        assertEquals(emptyList(), suggestions())
    }

    @Test
    fun suggestsTheLatestOfSeveralExpiredItems() = runTest {
        items.setItems(
            listOf(
                TestData.item(id = 1, name = "Yogurt", expiresInDays = -5),
                TestData.item(id = 2, name = "Yogurt", expiresInDays = -1),
            ),
        )

        assertEquals(listOf(today.minusDays(1)), suggestions().map { it.since })
    }

    @Test
    fun leavesOutWhatIsOnAListOrWasAddedToOneSince() = runTest {
        items.setItems(
            listOf(
                finished(1, "Bread", ItemStatus.CONSUMED, daysAgo = 2),
                finished(2, "Milk", ItemStatus.CONSUMED, daysAgo = 2),
                finished(3, "Butter", ItemStatus.CONSUMED, daysAgo = 2),
            ),
        )
        shopping.upsertItem(TestData.shoppingItem(name = "bread", checkedAt = TestTime.now))
        // Bought since it ran out, and the list cleared.
        shopping.rememberAdded("Milk", null, TestTime.now.minusSeconds(24 * 60 * 60))
        // Added before it ran out, so it's still suggested.
        shopping.rememberAdded("Butter", null, TestTime.now.minusSeconds(5 * 24 * 60 * 60))

        assertEquals(listOf("Butter"), suggestions().map { it.name })
    }

    @Test
    fun dismissedSuggestionsComeBackWhenTheProductRunsOutAgain() = runTest {
        val dismiss = DismissRestockSuggestionUseCase(settings, TestTime.clock)
        items.setItems(listOf(finished(1, "Bread", ItemStatus.CONSUMED, daysAgo = 2)))

        dismiss(suggestions().single())
        assertEquals(emptyList(), suggestions())

        items.setItems(listOf(finished(2, "Bread", ItemStatus.WASTED, daysAgo = 0)))
        assertEquals(listOf(RestockReason.THROWN_AWAY), suggestions().map { it.reason })
    }

    @Test
    fun dismissalsOfLongAgoAreForgotten() = runTest {
        settings.setDismissedRestockSuggestions(setOf("cheese|2020-01-01", "ham|${today.minusDays(3)}", "bad key"))
        val bread = RestockSuggestion("Bread", null, RestockReason.USED_UP, today, daysAgo = 0)

        DismissRestockSuggestionUseCase(settings, TestTime.clock)(bread)

        assertEquals(setOf("ham|${today.minusDays(3)}", bread.key), settings.current.dismissedRestockSuggestions)
    }
}
