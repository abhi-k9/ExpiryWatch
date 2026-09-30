package io.github.abhik9.expirywatch.core.data.repository

import io.github.abhik9.expirywatch.core.database.dao.ItemDao
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

internal class RoomItemRepository @Inject constructor(
    private val itemDao: ItemDao,
) : ItemRepository {
    override fun observeActiveItems(): Flow<List<Item>> =
        itemDao.observeActive().map { it.map(PopulatedItem::asExternalModel) }

    override fun observeFinishedItems(since: LocalDate): Flow<List<Item>> =
        itemDao.observeFinished(since).map { it.map(PopulatedItem::asExternalModel) }

    override fun observeItem(id: Long): Flow<Item?> = itemDao.observe(id).map { it?.asExternalModel() }

    override suspend fun getActiveItems(): List<Item> = itemDao.getActive().map(PopulatedItem::asExternalModel)

    override suspend fun upsert(item: Item): Long = if (item.id == 0L) {
        itemDao.insert(item.asEntity())
    } else {
        itemDao.update(item.asEntity())
        item.id
    }

    override suspend fun updateStatus(id: Long, status: ItemStatus, finishedDate: LocalDate?) =
        itemDao.updateStatus(id, status, finishedDate)

    override suspend fun delete(id: Long) = itemDao.delete(id)
}
