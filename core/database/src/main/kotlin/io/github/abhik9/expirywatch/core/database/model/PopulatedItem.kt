package io.github.abhik9.expirywatch.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.StorageLocation

/** An item together with its category and storage location. */
data class PopulatedItem(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
    @Relation(parentColumn = "location_id", entityColumn = "id")
    val location: LocationEntity?,
)

fun PopulatedItem.asExternalModel() = Item(
    id = item.id,
    name = item.name,
    brand = item.brand,
    barcode = item.barcode,
    category = category?.asExternalModel(),
    location = location?.asExternalModel(),
    quantity = item.quantity,
    unit = item.unit,
    expiryDate = item.expiryDate,
    openedDate = item.openedDate,
    useWithinDaysAfterOpening = item.useWithinDaysAfterOpening,
    notes = item.notes,
    imageUrl = item.imageUrl,
    status = item.status,
    finishedDate = item.finishedDate,
    createdAt = item.createdAt,
    updatedAt = item.updatedAt,
)

fun Item.asEntity() = ItemEntity(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    categoryId = category?.id,
    locationId = location?.id,
    quantity = quantity,
    unit = unit,
    expiryDate = expiryDate,
    openedDate = openedDate,
    useWithinDaysAfterOpening = useWithinDaysAfterOpening,
    notes = notes,
    imageUrl = imageUrl,
    status = status,
    finishedDate = finishedDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CategoryEntity.asExternalModel() = Category(id = id, name = name, emoji = emoji)

fun Category.asEntity() = CategoryEntity(id = id, name = name, emoji = emoji)

fun LocationEntity.asExternalModel() = StorageLocation(id = id, name = name, emoji = emoji)

fun StorageLocation.asEntity() = LocationEntity(id = id, name = name, emoji = emoji)

fun ProductEntity.asExternalModel() = ProductInfo(
    barcode = barcode,
    name = name,
    brand = brand,
    imageUrl = imageUrl,
    categoryId = categoryId,
    locationId = locationId,
    quantity = quantity,
    unit = unit,
    useWithinDaysAfterOpening = useWithinDaysAfterOpening,
    source = ProductSource.HISTORY,
)

fun ProductInfo.asEntity() = ProductEntity(
    barcode = barcode,
    name = name,
    brand = brand,
    imageUrl = imageUrl,
    categoryId = categoryId,
    locationId = locationId,
    quantity = quantity,
    unit = unit,
    useWithinDaysAfterOpening = useWithinDaysAfterOpening,
)
