package io.github.abhik9.expirywatch.core.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.abhik9.expirywatch.core.database.ExpiryWatchDatabase
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.CustomGroupEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import io.github.abhik9.expirywatch.core.database.model.PopulatedItem
import io.github.abhik9.expirywatch.core.database.model.PopulatedShoppingHistory
import io.github.abhik9.expirywatch.core.database.model.PopulatedShoppingItem
import io.github.abhik9.expirywatch.core.database.model.ProductEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingListEntity
import io.github.abhik9.expirywatch.core.database.model.asEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.domain.backup.ShoppingBackup
import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.BackupRepository
import java.io.IOException
import javax.inject.Inject

internal class RoomBackupRepository @Inject constructor(
    private val database: ExpiryWatchDatabase,
) : BackupRepository {
    override suspend fun createSnapshot(): BackupSnapshot = reportingFailures {
        database.withTransaction {
            BackupSnapshot(
                categories = database.categoryDao().getAll().map(CategoryEntity::asExternalModel),
                locations = database.locationDao().getAll().map(LocationEntity::asExternalModel),
                items = database.itemDao().getAllPopulated().map(PopulatedItem::asExternalModel),
                products = database.productDao().getAll().map(ProductEntity::asExternalModel),
                shopping = readShopping(),
                groups = database.customGroupDao().getAll().map(CustomGroupEntity::asExternalModel),
            )
        }
    }

    override suspend fun restore(snapshot: BackupSnapshot) = reportingFailures {
        database.withTransaction {
            // A backup from before shopping lists keeps the current ones.
            val shopping = snapshot.shopping ?: readShopping().relinkedTo(snapshot.categories, snapshot.locations)

            // Children first, so no foreign key points at a deleted row.
            database.shoppingDao().deleteAllHistory()
            database.shoppingDao().deleteAllLists()
            database.itemDao().deleteAll()
            database.productDao().deleteAll()
            database.categoryDao().deleteAll()
            database.locationDao().deleteAll()
            database.customGroupDao().deleteAll()

            database.categoryDao().insertAll(snapshot.categories.map { it.asEntity() })
            database.locationDao().insertAll(snapshot.locations.map { it.asEntity() })
            database.customGroupDao().insertAll(snapshot.groups.map { it.asEntity() })
            database.itemDao().insertAll(snapshot.items.map { it.asEntity() })
            database.productDao().insertAll(snapshot.products.map { it.asEntity() })
            database.shoppingDao().insertLists(shopping.lists.map { it.asEntity() })
            database.shoppingDao().insertItems(shopping.items.map { it.asEntity() })
            database.shoppingDao().insertHistory(shopping.history.map { it.asEntity(productKey(it.name)) })
        }
    }

    private suspend fun readShopping(): ShoppingBackup {
        val dao = database.shoppingDao()
        return ShoppingBackup(
            lists = dao.getLists().map(ShoppingListEntity::asExternalModel),
            items = dao.getAllItems().map(PopulatedShoppingItem::asExternalModel),
            history = dao.getAllHistory().map(PopulatedShoppingHistory::asExternalModel),
        )
    }

    /** The database reports problems such as a full disk as SQLiteExceptions. */
    private inline fun <T> reportingFailures(block: () -> T): T = try {
        block()
    } catch (e: SQLiteException) {
        throw IOException("Backup data couldn't be read or written", e)
    }
}
