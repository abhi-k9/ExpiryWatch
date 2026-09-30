package io.github.abhik9.expirywatch.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultDataCallbackTest {
    @Test
    fun newDatabasesStartWithDefaultCategoriesAndLocations() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, ExpiryWatchDatabase::class.java)
            .addCallback(DefaultDataCallback(context.resources))
            .allowMainThreadQueries()
            .build()

        val categories = database.categoryDao().getAll()
        val locations = database.locationDao().getAll()

        assertEquals(12, categories.size)
        assertTrue(categories.any { it.name == "Dairy & eggs" && it.emoji == "🧀" })
        assertEquals(5, locations.size)
        assertTrue(locations.any { it.name == "Fridge" })
        database.close()
    }
}
