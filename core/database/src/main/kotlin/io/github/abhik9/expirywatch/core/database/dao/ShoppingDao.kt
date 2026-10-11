package io.github.abhik9.expirywatch.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import io.github.abhik9.expirywatch.core.database.model.PopulatedShoppingHistory
import io.github.abhik9.expirywatch.core.database.model.PopulatedShoppingItem
import io.github.abhik9.expirywatch.core.database.model.ShoppingHistoryEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingItemEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingListEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_lists ORDER BY id")
    fun observeLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists ORDER BY id")
    suspend fun getLists(): List<ShoppingListEntity>

    @Insert
    suspend fun insertList(list: ShoppingListEntity): Long

    @Query("UPDATE shopping_lists SET name = :name WHERE id = :id")
    suspend fun renameList(id: Long, name: String)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Transaction
    @Query("SELECT * FROM shopping_items WHERE list_id = :listId")
    fun observeItems(listId: Long): Flow<List<PopulatedShoppingItem>>

    @Transaction
    @Query("SELECT * FROM shopping_items WHERE list_id = :listId")
    suspend fun getItems(listId: Long): List<PopulatedShoppingItem>

    @Insert
    suspend fun insertItem(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceItems(items: List<ShoppingItemEntity>)

    @Update
    suspend fun updateItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET checked_at = :checkedAt WHERE id IN (:ids)")
    suspend fun setCheckedAt(ids: List<Long>, checkedAt: Instant?)

    @Query("DELETE FROM shopping_items WHERE id IN (:ids)")
    suspend fun deleteItems(ids: List<Long>)

    @Transaction
    @Query("SELECT * FROM shopping_history ORDER BY times_added DESC, last_added_at DESC")
    fun observeHistory(): Flow<List<PopulatedShoppingHistory>>

    @Transaction
    @Query("SELECT * FROM shopping_history WHERE product_key = :productKey")
    suspend fun findInHistory(productKey: String): PopulatedShoppingHistory?

    @Upsert
    suspend fun upsertHistory(entry: ShoppingHistoryEntity)

    @Query("SELECT COUNT(*) FROM shopping_lists")
    suspend fun countLists(): Int

    @Query("SELECT COUNT(*) FROM shopping_items")
    suspend fun countItems(): Int
}
