package io.github.abhik9.expirywatch.feature.editor

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import io.github.abhik9.expirywatch.core.navigation.EditorNavKey
import io.github.abhik9.expirywatch.core.testing.TestData
import kotlin.test.assertEquals
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
