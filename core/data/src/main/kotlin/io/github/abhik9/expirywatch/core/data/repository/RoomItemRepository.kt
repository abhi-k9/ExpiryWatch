package io.github.abhik9.expirywatch.core.data.repository

import androidx.room.withTransaction
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.database.ExpiryWatchDatabase
import io.github.abhik9.expirywatch.core.database.model.PopulatedItem
import io.github.abhik9.expirywatch.core.database.model.asEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Records changes by id only: item names and notes are personal data. */
internal class RoomItemRepository @Inject constructor(
    private val database: ExpiryWatchDatabase,
    private val log: EventLog,
) : ItemRepository {
    private val itemDao = database.itemDao()

    override fun observeActiveItems(): Flow<List<Item>> =
        itemDao.observeActive().map { it.map(PopulatedItem::asExternalModel) }

    override fun observeFinishedItems(since: LocalDate): Flow<List<Item>> =
        itemDao.observeFinished(since).map { it.map(PopulatedItem::asExternalModel) }

    override fun observeItem(id: Long): Flow<Item?> = itemDao.observe(id).map { it?.asExternalModel() }

    override suspend fun getActiveItems(): List<Item> = itemDao.getActive().map(PopulatedItem::asExternalModel)

    override suspend fun upsert(item: Item): Long = if (item.id == 0L) {
        itemDao.insert(item.asEntity()).also { id -> log.record { "items: added #$id, expires ${item.expiryDate}" } }
    } else {
        itemDao.update(item.asEntity())
        log.record { "items: updated #${item.id}, expires ${item.expiryDate}" }
        item.id
    }

    override suspend fun upsertAll(items: List<Item>): List<Long> =
        database.withTransaction { items.map { upsert(it) } }

    override suspend fun updateStatus(id: Long, status: ItemStatus, finishedDate: LocalDate?) {
        itemDao.updateStatus(id, status, finishedDate)
        log.record { "items: #$id is now $status" }
    }

    override suspend fun delete(id: Long) {
        itemDao.delete(id)
        log.record { "items: deleted #$id" }
    }
}
