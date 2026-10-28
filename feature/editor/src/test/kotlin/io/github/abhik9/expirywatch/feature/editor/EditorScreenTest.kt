package io.github.abhik9.expirywatch.feature.editor

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import io.github.abhik9.expirywatch.core.navigation.EditorNavKey
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val fixture = EditorFixture()

    @Test
    fun savesAndStartsAnotherItemWithoutClosing() {
        val viewModel = fixture.viewModel()
        viewModel.updateForm {
            it.copy(
                name = "Milk",
                expiryDate = TestTime.today.plusDays(4),
                categoryId = TestData.dairy.id,
                locationId = TestData.fridge.id,
            )
        }
        var closed = false
        composeRule.setContent {
            EditorRoute(openScanner = false, onDone = { closed = true }, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Save & add another").performClick()
        composeRule.waitForIdle()

        assertFalse(closed)
        assertEquals("Milk", fixture.items.currentItems.single().name)
        assertEquals(EditorForm(categoryId = TestData.dairy.id, locationId = TestData.fridge.id), viewModel.form)
        composeRule.onNodeWithText("Saved Milk. Ready for another item.").assertExists()
    }

    @Test
    fun picksACategoryFromTheSheetAndCanClearIt() {
        val viewModel = fixture.viewModel()
        composeRule.setContent {
            EditorRoute(openScanner = false, onDone = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithContentDescription("Category").performScrollTo().performClick()
        composeRule.onNodeWithText("🥦 Produce").performClick()
        composeRule.runOnIdle { assertEquals(TestData.produce.id, viewModel.form.categoryId) }

        composeRule.onNodeWithContentDescription("Category").performClick()
        // There is also a None value in the underlying storage field.
        composeRule.onNode(hasText("None") and isNotSelected()).performClick()
        composeRule.runOnIdle { assertEquals(null, viewModel.form.categoryId) }
    }

    @Test
    fun aRecentCategoryCanBeSelectedBesideTheField() {
        runBlocking { fixture.items.upsert(TestData.item(id = 5)) }
        val viewModel = fixture.viewModel()
        composeRule.setContent {
            EditorRoute(openScanner = false, onDone = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("🥛 Dairy").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(TestData.dairy.id, viewModel.form.categoryId) }
    }

    @Test
    fun editsAnItemAndCloses() {
        runBlocking { fixture.items.upsert(TestData.item(id = 5, name = "Milk", expiresInDays = 4)) }
        val viewModel = fixture.viewModel(EditorNavKey(itemId = 5))
        var closed = false
        composeRule.setContent {
            EditorRoute(openScanner = false, onDone = { closed = true }, viewModel = viewModel)
        }

        composeRule.onNode(hasSetTextAction() and hasText("Milk")).performTextReplacement("Oat milk")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        assertTrue(closed)
        assertEquals("Oat milk", fixture.items.currentItems.single().name)
    }
}
