package io.github.abhik9.expirywatch.feature.items

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import io.github.abhik9.expirywatch.core.domain.usecase.DeleteItemUseCase
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ItemsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fixture = ItemsFixture()
    private var settingsOpened = false

    @Test
    @Config(qualifiers = "w400dp-h900dp")
    fun searchIsReachableAtTheBottomAndCanFilterClearAndClose() {
        showItems()

        val search = composeRule.onNodeWithContentDescription("Search")
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(search.fetchSemanticsNode().boundsInRoot.center.y > screen.center.y)
        search.performClick()
        val field = composeRule.onNodeWithText("Search items")
        assertTrue(field.fetchSemanticsNode().boundsInRoot.center.y > screen.center.y)
        field.performTextInput("Milk")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Apples").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNode(hasText("Milk") and hasSetTextAction().not()).assertExists()

        composeRule.onNodeWithContentDescription("Clear search").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Apples").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Close search").performClick()
        composeRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        composeRule.onNodeWithText("Search items").assertDoesNotExist()
    }

    private fun showItems() {
        val viewModel = fixture.viewModel()
        composeRule.setContent {
            ItemsRoute(
                onItemClick = {},
                onAddItem = {},
                onScanItem = {},
                onOpenShoppingList = {},
                onOpenSettings = { settingsOpened = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun aLongPressSelectsAnItemWhoseRemindersCanThenBeSnoozed() {
        showItems()

        composeRule.onNodeWithText("Milk").performTouchInput { longClick() }
        composeRule.onNodeWithText("1 selected").assertExists()
        composeRule.onNodeWithContentDescription("Snooze reminders").performClick()
        composeRule.onNodeWithText("For a week").performClick()
        composeRule.waitForIdle()

        val milk = runBlocking { fixture.items.observeItem(1).first() }
        assertEquals(TestTime.today.plusWeeks(1), milk?.remindersSnoozedUntil)
        composeRule.onNodeWithText("1 selected").assertDoesNotExist()
    }

    @Test
    fun warnsWhenRemindersCantArriveAndPointsToTheSettings() {
        fixture.reminderHealth.health = fixture.reminderHealth.health.copy(notificationsAllowed = false)
        showItems()

        composeRule.onNodeWithText("Reminders may not arrive").assertExists()
        composeRule.onNodeWithText("Fix").performClick()

        assertTrue(settingsOpened)
    }

    @Test
    fun aDeletedItemCanBePutBack() {
        showItems()
        val milk = runBlocking { fixture.items.observeItem(1).first()!! }

        runBlocking { DeleteItemUseCase(fixture.items, fixture.recentlyDeleted)(milk) }
        composeRule.onNodeWithText("Deleted Milk").assertExists()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()

        assertEquals(milk, runBlocking { fixture.items.observeItem(1).first() })
        composeRule.onNodeWithText("Milk").assertExists()
    }

    @Test
    fun swipingAnItemToTheEndMarksItUsedUpAndUndoPutsItBack() {
        showItems()

        // Well past the threshold; the text itself is narrower than the row.
        composeRule.onNodeWithText("Milk").performTouchInput { swipeRight(startX = left, endX = left + 1000f) }
        composeRule.waitForIdle()
        assertEquals(ItemStatus.CONSUMED, runBlocking { fixture.items.observeItem(1).first()?.status })

        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()
        assertEquals(ItemStatus.ACTIVE, runBlocking { fixture.items.observeItem(1).first()?.status })
        // Back in its place, rather than swiped away, and not finished again.
        composeRule.onNodeWithText("Milk").assertIsDisplayed()
        assertEquals(ItemStatus.ACTIVE, runBlocking { fixture.items.observeItem(1).first()?.status })
    }
}
