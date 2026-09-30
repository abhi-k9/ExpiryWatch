package io.github.abhik9.expirywatch.core.navigation

import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.common.navigation.DeepLink
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class NavigatorTest {
    private val backStack = mutableListOf<NavKey>()
    private val navigator = Navigator(backStack)

    @Test
    fun startsAtTheItemList() {
        assertEquals(listOf<NavKey>(ItemsNavKey()), backStack)
        assertFalse(navigator.goBack())
    }

    @Test
    fun topLevelScreensSitOnTopOfTheList() {
        navigator.navigate(InsightsNavKey)
        navigator.navigate(SettingsNavKey)

        assertEquals(listOf(ItemsNavKey(), SettingsNavKey), backStack)
        assertEquals(SettingsNavKey, navigator.currentTopLevelKey)

        assertTrue(navigator.goBack())
        assertEquals(listOf<NavKey>(ItemsNavKey()), backStack)
    }

    @Test
    fun detailScreensStackAndKeepTheirTopLevel() {
        navigator.navigate(SettingsNavKey)
        navigator.navigate(ManageLabelsNavKey(LabelKind.CATEGORIES))

        assertEquals(SettingsNavKey, navigator.currentTopLevelKey)
        assertEquals(ManageLabelsNavKey(LabelKind.CATEGORIES), navigator.currentKey)
    }

    @Test
    fun reselectingTheListClearsItsFilter() {
        navigator.handleDeepLink(DeepLink.Items(ExpiryStatus.EXPIRED))
        navigator.navigate(InsightsNavKey)
        navigator.navigate(ItemsNavKey())
        // Coming back from another tab keeps the filtered list...
        assertEquals(listOf<NavKey>(ItemsNavKey(ExpiryStatus.EXPIRED)), backStack)

        // ...re-selecting it while on it resets the filter.
        navigator.navigate(ItemsNavKey())
        assertEquals(listOf<NavKey>(ItemsNavKey()), backStack)
    }

    @Test
    fun deepLinksReplaceTheBackStack() {
        navigator.navigate(SettingsNavKey)

        navigator.handleDeepLink(DeepLink.EditItem(itemId = 3))
        assertEquals(listOf(ItemsNavKey(), EditorNavKey(itemId = 3)), backStack)

        navigator.handleDeepLink(DeepLink.AddItem(scanBarcode = true))
        assertEquals(listOf(ItemsNavKey(), EditorNavKey(scanBarcode = true)), backStack)
    }
}
