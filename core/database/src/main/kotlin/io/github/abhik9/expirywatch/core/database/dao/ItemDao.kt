package io.github.abhik9.expirywatch.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.abhik9.expirywatch.core.database.model.ItemEntity
import io.github.abhik9.expirywatch.core.database.model.PopulatedItem
import io.github.abhik9.expirywatch.core.model.ItemStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Transaction
    @Query("SELECT * FROM items WHERE status = 'ACTIVE'")
    fun observeActive(): Flow<List<PopulatedItem>>

    @Transaction
    @Query("SELECT * FROM items WHERE status = 'ACTIVE'")
    suspend fun getActive(): List<PopulatedItem>

    @Transaction
    @Query("SELECT * FROM items WHERE status != 'ACTIVE' AND finished_date >= :since")
    fun observeFinished(since: LocalDate): Flow<List<PopulatedItem>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<PopulatedItem?>

    @Transaction
    @Query("SELECT * FROM items")
    suspend fun getAllPopulated(): List<PopulatedItem>

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET status = :status, finished_date = :finishedDate WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ItemStatus, finishedDate: LocalDate?)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}
