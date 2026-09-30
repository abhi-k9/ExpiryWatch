package io.github.abhik9.expirywatch.core.database

import android.content.res.Resources
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Fills a newly created database with a starter set of categories and storage locations, in the
 * user's language. Users can rename or delete them afterwards.
 */
internal class DefaultDataCallback(private val resources: Resources) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DEFAULT_CATEGORIES.forEach { (nameRes, emoji) ->
            db.execSQL(
                "INSERT INTO categories (name, emoji) VALUES (?, ?)",
                arrayOf(resources.getString(nameRes), emoji),
            )
        }
        DEFAULT_LOCATIONS.forEach { (nameRes, emoji) ->
            db.execSQL(
                "INSERT INTO locations (name, emoji) VALUES (?, ?)",
                arrayOf(resources.getString(nameRes), emoji),
            )
        }
    }

    private companion object {
        val DEFAULT_CATEGORIES = listOf(
            R.string.core_database_category_dairy to "🧀",
            R.string.core_database_category_meat_fish to "🍗",
            R.string.core_database_category_produce to "🥕",
            R.string.core_database_category_bakery to "🍞",
            R.string.core_database_category_dry_goods to "🥫",
            R.string.core_database_category_frozen to "🍨",
            R.string.core_database_category_drinks to "🧃",
            R.string.core_database_category_snacks to "🍫",
            R.string.core_database_category_condiments to "🧂",
            R.string.core_database_category_medicine to "💊",
            R.string.core_database_category_personal_care to "🧴",
            R.string.core_database_category_other to "📦",
        )
        val DEFAULT_LOCATIONS = listOf(
            R.string.core_database_location_fridge to "🧊",
            R.string.core_database_location_freezer to "❄️",
            R.string.core_database_location_pantry to "🗄️",
            R.string.core_database_location_cupboard to "🍽️",
            R.string.core_database_location_bathroom to "🪥",
        )
    }
}
