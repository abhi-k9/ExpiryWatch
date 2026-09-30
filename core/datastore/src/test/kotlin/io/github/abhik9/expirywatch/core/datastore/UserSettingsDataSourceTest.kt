package io.github.abhik9.expirywatch.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserSettingsDataSourceTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val dataSource = UserSettingsDataSource(
        PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) {
            tmpFolder.newFile("user_settings.preferences_pb")
        },
    )

    @Test
    fun emptyStoreYieldsDefaults() = testScope.runTest {
        assertEquals(UserSettings(), dataSource.settings.first())
    }

    @Test
    fun writtenValuesAreReadBack() = testScope.runTest {
        dataSource.setThemeMode(ThemeMode.DARK)
        dataSource.setUseDynamicColor(false)
        dataSource.setExpiringSoonDays(5)
        dataSource.setRemindersEnabled(false)
        dataSource.setReminderTime(LocalTime.of(18, 45))
        dataSource.setSortOrder(ItemSortOrder.NAME)

        assertEquals(
            UserSettings(
                themeMode = ThemeMode.DARK,
                useDynamicColor = false,
                expiringSoonDays = 5,
                remindersEnabled = false,
                reminderTime = LocalTime.of(18, 45),
                sortOrder = ItemSortOrder.NAME,
            ),
            dataSource.settings.first(),
        )
    }

    @Test
    fun expiringSoonDaysAreClampedToTheSupportedRange() = testScope.runTest {
        dataSource.setExpiringSoonDays(1000)
        assertEquals(UserSettings.EXPIRING_SOON_DAYS_RANGE.last, dataSource.settings.first().expiringSoonDays)
    }
}
