package io.github.abhik9.expirywatch.feature.settings.advanced

import io.github.abhik9.expirywatch.core.model.ProductGrouping
import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AdvancedSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeUserSettingsRepository()

    // Lazy, so the ViewModel is created after MainDispatcherRule has replaced the main dispatcher.
    private val viewModel by lazy { AdvancedSettingsViewModel(settings) }

    @Test
    fun showsTheDefaults() = runTest {
        assertEquals(UserSettings(), viewModel.settings.filterNotNull().first())
    }

    @Test
    fun changesAreSaved() = runTest {
        viewModel.setProductGrouping(ProductGrouping.NAME_AND_BRAND)
        viewModel.setKeepProductsTogether(true)
        viewModel.setRemindAboutExpired(false)
        viewModel.setOnlineProductLookup(false)

        assertEquals(
            UserSettings(
                productGrouping = ProductGrouping.NAME_AND_BRAND,
                keepProductsTogether = true,
                remindAboutExpired = false,
                onlineProductLookup = false,
            ),
            settings.current,
        )
    }
}
