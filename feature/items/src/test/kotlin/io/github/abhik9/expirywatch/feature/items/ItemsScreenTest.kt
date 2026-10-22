package io.github.abhik9.expirywatch.feature.items

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.abhik9.expirywatch.core.domain.usecase.DeleteItemUseCase
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fixture = ItemsFixture()
    private var settingsOpened = false

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
}
