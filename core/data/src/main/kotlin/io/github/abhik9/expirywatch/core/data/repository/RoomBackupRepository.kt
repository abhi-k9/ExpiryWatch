package io.github.abhik9.expirywatch.core.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.abhik9.expirywatch.core.database.ExpiryWatchDatabase
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import io.github.abhik9.expirywatch.core.database.model.PopulatedItem
import io.github.abhik9.expirywatch.core.database.model.ProductEntity
import io.github.abhik9.expirywatch.core.database.model.asEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
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
            )
        }
    }

    override suspend fun restore(snapshot: BackupSnapshot) = reportingFailures {
        database.withTransaction {
            // Children first, so no foreign key points at a deleted row.
            database.itemDao().deleteAll()
            database.productDao().deleteAll()
            database.categoryDao().deleteAll()
            database.locationDao().deleteAll()

            database.categoryDao().insertAll(snapshot.categories.map { it.asEntity() })
            database.locationDao().insertAll(snapshot.locations.map { it.asEntity() })
            database.itemDao().insertAll(snapshot.items.map { it.asEntity() })
            database.productDao().insertAll(snapshot.products.map { it.asEntity() })
        }
    }

    /** The database reports problems such as a full disk as SQLiteExceptions. */
    private inline fun <T> reportingFailures(block: () -> T): T = try {
        block()
    } catch (e: SQLiteException) {
        throw IOException("Backup data couldn't be read or written", e)
    }
}
