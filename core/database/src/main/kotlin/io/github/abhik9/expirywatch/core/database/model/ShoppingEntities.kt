package io.github.abhik9.expirywatch.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.ShoppingList
import java.time.Instant

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
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
        Index("list_id"),
        Index("category_id"),
        Index("location_id"),
    ],
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "list_id") val listId: Long,
    val name: String,
    val brand: String?,
    val barcode: String?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "location_id") val locationId: Long?,
    val quantity: Double,
    val unit: QuantityUnit,
    val note: String?,
    @ColumnInfo(name = "checked_at") val checkedAt: Instant?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

/** A product added to shopping lists before, keyed by its name ignoring case, accents and spacing. */
@Entity(
    tableName = "shopping_history",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("category_id")],
)
data class ShoppingHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "product_key") val productKey: String,
    val name: String,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "times_added") val timesAdded: Int,
    @ColumnInfo(name = "last_added_at") val lastAddedAt: Instant,
)

/** A shopping item together with its category and storage location. */
data class PopulatedShoppingItem(
    @Embedded val item: ShoppingItemEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "location_id", entityColumn = "id")
    val location: LocationEntity?,
)

data class PopulatedShoppingHistory(
    @Embedded val entry: ShoppingHistoryEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
)

fun ShoppingListEntity.asExternalModel() = ShoppingList(id = id, name = name, createdAt = createdAt)

fun PopulatedShoppingItem.asExternalModel() = ShoppingItem(
    id = item.id,
    listId = item.listId,
    name = item.name,
    brand = item.brand,
    barcode = item.barcode,
    category = category?.asExternalModel(),
    location = location?.asExternalModel(),
    quantity = item.quantity,
    unit = item.unit,
    note = item.note,
    checkedAt = item.checkedAt,
    createdAt = item.createdAt,
)

fun ShoppingItem.asEntity() = ShoppingItemEntity(
    id = id,
    listId = listId,
    name = name,
    brand = brand,
    barcode = barcode,
    categoryId = category?.id,
    locationId = location?.id,
    quantity = quantity,
    unit = unit,
    note = note,
    checkedAt = checkedAt,
    createdAt = createdAt,
)

fun PopulatedShoppingHistory.asExternalModel() = ShoppingHistoryEntry(
    name = entry.name,
    category = category?.asExternalModel(),
    timesAdded = entry.timesAdded,
    lastAddedAt = entry.lastAddedAt,
)
