package io.github.abhik9.expirywatch.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations")
    suspend fun getAll(): List<LocationEntity>

    @Insert
    suspend fun insert(location: LocationEntity): Long

    @Insert
    suspend fun insertAll(locations: List<LocationEntity>)

    @Update
    suspend fun update(location: LocationEntity)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM locations")
    suspend fun deleteAll()
}
