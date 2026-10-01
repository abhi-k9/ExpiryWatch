package io.github.abhik9.expirywatch.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import io.github.abhik9.expirywatch.core.database.model.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun find(barcode: String): ProductEntity?

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Insert
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}
