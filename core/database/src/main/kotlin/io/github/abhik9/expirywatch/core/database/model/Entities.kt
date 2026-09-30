package io.github.abhik9.expirywatch.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("category_id"),
        Index("location_id"),
        Index("status", "finished_date"),
    ],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String?,
    val barcode: String?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "location_id") val locationId: Long?,
    val quantity: Double,
    val unit: QuantityUnit,
    @ColumnInfo(name = "expiry_date") val expiryDate: LocalDate,
    @ColumnInfo(name = "opened_date") val openedDate: LocalDate?,
    @ColumnInfo(name = "use_within_days") val useWithinDaysAfterOpening: Int?,
    val notes: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    val status: ItemStatus,
    @ColumnInfo(name = "finished_date") val finishedDate: LocalDate?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)

/** What the user entered the last time they saved an item with this barcode. */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("category_id"), Index("location_id")],
)
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "location_id") val locationId: Long?,
    val quantity: Double?,
    val unit: QuantityUnit?,
    @ColumnInfo(name = "use_within_days") val useWithinDaysAfterOpening: Int?,
)
