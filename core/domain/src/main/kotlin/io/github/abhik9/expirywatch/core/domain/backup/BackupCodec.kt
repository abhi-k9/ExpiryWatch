package io.github.abhik9.expirywatch.core.domain.backup

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.usecase.ItemValidator
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.model.StorageLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Converts a [BackupSnapshot] to and from a versioned JSON document.
 *
 * The file has its own DTOs rather than serializing the domain models directly, so a refactor
 * of the models can't silently change the file format. Bump [CURRENT_VERSION] and add a
 * migration in [decode] when the format changes. Version 2 added the shopping lists, so a
 * version 1 file reads as having none at all, rather than none left.
 */
object BackupCodec {
    const val FORMAT = "expirywatch-backup"
    const val CURRENT_VERSION = 2
    const val MIME_TYPE = "application/json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(snapshot: BackupSnapshot, exportedAt: Instant): String = json.encodeToString(
        BackupFileDto.serializer(),
        BackupFileDto(
            format = FORMAT,
            version = CURRENT_VERSION,
            exportedAt = exportedAt.toString(),
            categories = snapshot.categories.map { CategoryDto(it.id, it.name, it.emoji) },
            locations = snapshot.locations.map { LocationDto(it.id, it.name, it.emoji) },
            items = snapshot.items.map { it.toDto() },
            products = snapshot.products.map { it.toDto() },
            shopping = snapshot.shopping?.toDto(),
            groups = snapshot.groups.map { GroupDto(it.id, it.name, it.emoji) },
        ),
    )

    /** @throws BackupFormatException when [text] is not a valid backup this version can read. */
    fun decode(text: String): BackupSnapshot {
        val file = try {
            json.decodeFromString(BackupFileDto.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("Not an ExpiryWatch backup file", e)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("Not an ExpiryWatch backup file", e)
        }
        if (file.format != FORMAT) throw BackupFormatException("Unknown file format '${file.format}'")
        if (file.version !in 1..CURRENT_VERSION) {
            throw BackupFormatException("Backup version ${file.version} is newer than this app supports")
        }

        return try {
            file.toSnapshot()
        } catch (e: DateTimeParseException) {
            throw BackupFormatException("Backup contains an invalid date", e)
        }
    }

    private fun BackupFileDto.toSnapshot(): BackupSnapshot {
        // Rows keep their ids, so items can refer to categories and locations by id.
        requireValidIds(categories.map { it.id }, "categories")
        requireValidIds(locations.map { it.id }, "locations")
        requireValidIds(items.map { it.id }, "items")
        requireValidIds(groups.map { it.id }, "groups")

        val categoriesById = categories
            .map { Category(id = it.id, name = it.name.trim(), emoji = it.emoji) }
            .filter { it.name.isNotEmpty() }
            .associateBy { it.id }
        val locationsById = locations
            .map { StorageLocation(id = it.id, name = it.name.trim(), emoji = it.emoji) }
            .filter { it.name.isNotEmpty() }
            .associateBy { it.id }
        val groupsById = groups
            .map { CustomGroup(id = it.id, name = it.name.trim(), emoji = it.emoji) }
            .filter { it.name.isNotEmpty() }
            .associateBy { it.id }

        return BackupSnapshot(
            categories = categoriesById.values.toList(),
            locations = locationsById.values.toList(),
            items = items.map { dto ->
                if (dto.name.isBlank()) throw BackupFormatException("Backup contains an item without a name")
                Item(
                    id = dto.id,
                    name = dto.name.trim(),
                    brand = dto.brand,
                    barcode = dto.barcode,
                    // References to categories or locations missing from the file are dropped.
                    category = dto.categoryId?.let(categoriesById::get),
                    location = dto.locationId?.let(locationsById::get),
                    quantity = dto.quantity.takeIf(::isValidQuantity) ?: 1.0,
                    unit = enumValueOrDefault(dto.unit, QuantityUnit.PIECES),
                    expiryDate = LocalDate.parse(dto.expiryDate),
                    openedDate = dto.openedDate?.let(LocalDate::parse),
                    useWithinDaysAfterOpening = dto.useWithinDaysAfterOpening
                        ?.takeIf { it in ItemValidator.OPENED_WINDOW_DAYS_RANGE },
                    notes = dto.notes,
                    imageUrl = dto.imageUrl,
                    status = enumValueOrDefault(dto.status, ItemStatus.ACTIVE),
                    finishedDate = dto.finishedDate?.let(LocalDate::parse),
                    createdAt = dto.createdAt?.let(::parseInstant) ?: Instant.EPOCH,
                    updatedAt = dto.updatedAt?.let(::parseInstant) ?: Instant.EPOCH,
                    remindersSnoozedUntil = dto.remindersSnoozedUntil?.let(LocalDate::parse),
                    customGroup = dto.groupId?.let(groupsById::get),
                    remindDaysBefore = dto.remindDaysBefore?.takeIf { it in ItemValidator.REMIND_DAYS_BEFORE_RANGE },
                    price = dto.price?.takeIf { it.isFinite() && it in 0.0..ItemValidator.MAX_PRICE },
                ).let { item ->
                    // An active item has no finish date; a finished one needs one for insights.
                    when {
                        item.status == ItemStatus.ACTIVE -> item.copy(finishedDate = null)
                        item.finishedDate == null -> item.copy(finishedDate = item.updatedAt.toLocalDateOrNull())
                        else -> item
                    }
                }
            },
            products = products
                .filter { it.barcode.isNotBlank() && it.name.isNotBlank() }
                .distinctBy { it.barcode }
                .map { dto ->
                    ProductInfo(
                        barcode = dto.barcode,
                        name = dto.name,
                        brand = dto.brand,
                        imageUrl = dto.imageUrl,
                        categoryId = dto.categoryId?.takeIf(categoriesById::containsKey),
                        locationId = dto.locationId?.takeIf(locationsById::containsKey),
                        quantity = dto.quantity?.takeIf(::isValidQuantity),
                        unit = dto.unit?.let { enumValueOrDefault(it, QuantityUnit.PIECES) },
                        useWithinDaysAfterOpening = dto.useWithinDaysAfterOpening
                            ?.takeIf { it in ItemValidator.OPENED_WINDOW_DAYS_RANGE },
                        source = ProductSource.HISTORY,
                    )
                },
            shopping = shopping?.toBackup(categoriesById, locationsById),
            groups = groupsById.values.toList(),
        )
    }

    private fun ShoppingDto.toBackup(
        categoriesById: Map<Long, Category>,
        locationsById: Map<Long, StorageLocation>,
    ): ShoppingBackup {
        requireValidIds(lists.map { it.id }, "shopping lists")
        requireValidIds(items.map { it.id }, "shopping items")
        val listIds = lists.map { it.id }.toSet()
        return ShoppingBackup(
            lists = lists.map { dto ->
                if (dto.name.isBlank()) throw BackupFormatException("Backup contains a shopping list without a name")
                ShoppingList(
                    id = dto.id,
                    name = dto.name.trim(),
                    createdAt = dto.createdAt?.let(::parseInstant) ?: Instant.EPOCH,
                )
            },
            // Items on a list missing from the file have nowhere to go.
            items = items.filter { it.listId in listIds }.map { dto ->
                if (dto.name.isBlank()) throw BackupFormatException("Backup contains a shopping item without a name")
                ShoppingItem(
                    id = dto.id,
                    listId = dto.listId,
                    name = dto.name.trim(),
                    brand = dto.brand,
                    barcode = dto.barcode,
                    category = dto.categoryId?.let(categoriesById::get),
                    location = dto.locationId?.let(locationsById::get),
                    quantity = dto.quantity.takeIf(::isValidQuantity) ?: 1.0,
                    unit = enumValueOrDefault(dto.unit, QuantityUnit.PIECES),
                    note = dto.note,
                    checkedAt = dto.checkedAt?.let(::parseInstant),
                    createdAt = dto.createdAt?.let(::parseInstant) ?: Instant.EPOCH,
                )
            },
            // Products are remembered by name, so the first of each name is kept.
            history = history
                .filter { productKey(it.name).isNotEmpty() }
                .distinctBy { productKey(it.name) }
                .map { dto ->
                    ShoppingHistoryEntry(
                        name = dto.name.trim(),
                        category = dto.categoryId?.let(categoriesById::get),
                        timesAdded = dto.timesAdded.coerceAtLeast(1),
                        lastAddedAt = dto.lastAddedAt?.let(::parseInstant) ?: Instant.EPOCH,
                    )
                },
            aisleOrder = aisleOrder.filter(categoriesById::containsKey).distinct(),
        )
    }

    private fun requireValidIds(ids: List<Long>, kind: String) {
        if (ids.any { it <= 0 }) throw BackupFormatException("Backup contains $kind with invalid ids")
        if (ids.toSet().size != ids.size) throw BackupFormatException("Backup contains duplicate $kind")
    }

    private fun isValidQuantity(quantity: Double): Boolean =
        quantity.isFinite() && quantity > 0 && quantity <= ItemValidator.MAX_QUANTITY

    private fun parseInstant(value: String): Instant = try {
        Instant.parse(value)
    } catch (e: DateTimeParseException) {
        throw BackupFormatException("Backup contains an invalid timestamp", e)
    }

    private fun Instant.toLocalDateOrNull(): LocalDate? =
        takeIf { it != Instant.EPOCH }?.let { LocalDate.ofInstant(it, ZoneOffset.UTC) }

    private inline fun <reified E : Enum<E>> enumValueOrDefault(name: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private fun Item.toDto() = ItemDto(
        id = id,
        name = name,
        brand = brand,
        barcode = barcode,
        categoryId = category?.id,
        locationId = location?.id,
        quantity = quantity,
        unit = unit.name,
        expiryDate = expiryDate.toString(),
        openedDate = openedDate?.toString(),
        useWithinDaysAfterOpening = useWithinDaysAfterOpening,
        notes = notes,
        imageUrl = imageUrl,
        status = status.name,
        finishedDate = finishedDate?.toString(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        remindersSnoozedUntil = remindersSnoozedUntil?.toString(),
        groupId = customGroup?.id,
        remindDaysBefore = remindDaysBefore,
        price = price,
    )

    private fun ShoppingBackup.toDto() = ShoppingDto(
        lists = lists.map { ShoppingListDto(id = it.id, name = it.name, createdAt = it.createdAt.toString()) },
        items = items.map { it.toDto() },
        history = history.map {
            ShoppingHistoryDto(
                name = it.name,
                categoryId = it.category?.id,
                timesAdded = it.timesAdded,
                lastAddedAt = it.lastAddedAt.toString(),
            )
        },
        aisleOrder = aisleOrder,
    )

    private fun ShoppingItem.toDto() = ShoppingItemDto(
        id = id,
        listId = listId,
        name = name,
        brand = brand,
        barcode = barcode,
        categoryId = category?.id,
        locationId = location?.id,
        quantity = quantity,
        unit = unit.name,
        note = note,
        checkedAt = checkedAt?.toString(),
        createdAt = createdAt.toString(),
    )

    private fun ProductInfo.toDto() = ProductDto(
        barcode = barcode,
        name = name,
        brand = brand,
        imageUrl = imageUrl,
        categoryId = categoryId,
        locationId = locationId,
        quantity = quantity,
        unit = unit?.name,
        useWithinDaysAfterOpening = useWithinDaysAfterOpening,
    )
}

@Serializable
private data class BackupFileDto(
    val format: String,
    val version: Int,
    val exportedAt: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val locations: List<LocationDto> = emptyList(),
    val items: List<ItemDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val shopping: ShoppingDto? = null,
    val groups: List<GroupDto> = emptyList(),
)

@Serializable
private data class CategoryDto(val id: Long, val name: String, val emoji: String = "")

@Serializable
private data class LocationDto(val id: Long, val name: String, val emoji: String = "")

@Serializable
private data class GroupDto(val id: Long, val name: String, val emoji: String = "")

@Serializable
private data class ItemDto(
    val id: Long,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Double = 1.0,
    val unit: String = QuantityUnit.PIECES.name,
    val expiryDate: String,
    val openedDate: String? = null,
    @SerialName("useWithinDays")
    val useWithinDaysAfterOpening: Int? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    val status: String = ItemStatus.ACTIVE.name,
    val finishedDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val remindersSnoozedUntil: String? = null,
    val groupId: Long? = null,
    val remindDaysBefore: Int? = null,
    val price: Double? = null,
)

@Serializable
private data class ProductDto(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    @SerialName("useWithinDays")
    val useWithinDaysAfterOpening: Int? = null,
)

@Serializable
private data class ShoppingDto(
    val lists: List<ShoppingListDto> = emptyList(),
    val items: List<ShoppingItemDto> = emptyList(),
    val history: List<ShoppingHistoryDto> = emptyList(),
    val aisleOrder: List<Long> = emptyList(),
)

@Serializable
private data class ShoppingListDto(val id: Long, val name: String, val createdAt: String? = null)

@Serializable
private data class ShoppingItemDto(
    val id: Long,
    val listId: Long,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Double = 1.0,
    val unit: String = QuantityUnit.PIECES.name,
    val note: String? = null,
    val checkedAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
private data class ShoppingHistoryDto(
    val name: String,
    val categoryId: Long? = null,
    val timesAdded: Int = 1,
    val lastAddedAt: String? = null,
)
