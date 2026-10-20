package io.github.abhik9.expirywatch.core.domain.backup

import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupCodecTest {
    private val camping = CustomGroup(id = 5, name = "Camping kit", emoji = "⛺")
    private val snapshot = BackupSnapshot(
        categories = listOf(TestData.dairy, TestData.produce),
        locations = listOf(TestData.fridge, TestData.pantry),
        items = listOf(
            TestData.item(id = 1, name = "Milk").copy(
                barcode = "4001234567890",
                quantity = 1.5,
                unit = QuantityUnit.LITERS,
                openedDate = TestTime.today,
                useWithinDaysAfterOpening = 4,
                notes = "Organic",
                remindersSnoozedUntil = TestTime.today.plusDays(3),
                customGroup = camping,
            ),
            TestData.item(id = 2, name = "Spinach", category = TestData.produce, location = null).copy(
                status = ItemStatus.WASTED,
                finishedDate = TestTime.today,
                remindersSnoozedUntil = Item.SNOOZED_INDEFINITELY,
            ),
        ),
        products = listOf(
            ProductInfo(
                barcode = "4001234567890",
                name = "Milk",
                categoryId = 1,
                quantity = 1.5,
                unit = QuantityUnit.LITERS,
                source = ProductSource.HISTORY,
            ),
        ),
        shopping = ShoppingBackup(
            lists = listOf(
                ShoppingList(id = 1, name = "Groceries", createdAt = TestTime.now),
                ShoppingList(id = 3, name = "Pharmacy", createdAt = TestTime.now),
            ),
            items = listOf(
                TestData.shoppingItem(id = 1, name = "Butter").copy(
                    brand = "Kerrygold",
                    barcode = "5011038133484",
                    location = TestData.fridge,
                    note = "unsalted",
                ),
                TestData.shoppingItem(
                    id = 2,
                    name = "Plasters",
                    listId = 3,
                    category = null,
                    quantity = 2.0,
                    unit = QuantityUnit.PACKS,
                    checkedAt = TestTime.now,
                ),
            ),
            history = listOf(
                ShoppingHistoryEntry(
                    name = "Butter",
                    category = TestData.dairy,
                    timesAdded = 3,
                    lastAddedAt = TestTime.now,
                ),
            ),
            aisleOrder = listOf(2L, 1L),
        ),
        groups = listOf(camping),
    )

    @Test
    fun roundTripsASnapshot() {
        val decoded = BackupCodec.decode(BackupCodec.encode(snapshot, exportedAt = TestTime.now))

        assertEquals(snapshot.categories, decoded.categories)
        assertEquals(snapshot.locations, decoded.locations)
        assertEquals(snapshot.items, decoded.items)
        assertEquals(snapshot.products, decoded.products)
        assertEquals(snapshot.shopping, decoded.shopping)
        assertEquals(snapshot.groups, decoded.groups)
    }

    @Test
    fun writesAVersionedDocument() {
        val json = BackupCodec.encode(snapshot, exportedAt = TestTime.now)

        assertTrue(json.contains("\"format\": \"expirywatch-backup\""))
        assertTrue(json.contains("\"version\": 2"))
    }

    @Test
    fun readsBackupsFromBeforeShoppingLists() {
        val decoded = BackupCodec.decode(
            """{"format": "expirywatch-backup", "version": 1, "categories": [{"id": 1, "name": "Dairy"}]}""",
        )

        assertEquals(listOf("Dairy"), decoded.categories.map { it.name })
        // No shopping lists at all, rather than none left.
        assertNull(decoded.shopping)
        assertEquals(
            ShoppingBackup(),
            BackupCodec.decode("""{"format": "expirywatch-backup", "version": 2, "shopping": {}}""").shopping,
        )
    }

    @Test
    fun rejectsOtherDocuments() {
        assertFailsWith<BackupFormatException> { BackupCodec.decode("not json") }
        assertFailsWith<BackupFormatException> { BackupCodec.decode("""{"hello": "world"}""") }
        assertFailsWith<BackupFormatException> { BackupCodec.decode("""{"format": "other", "version": 1}""") }
    }

    @Test
    fun rejectsNewerVersions() {
        assertFailsWith<BackupFormatException> {
            BackupCodec.decode("""{"format": "expirywatch-backup", "version": 99}""")
        }
    }

    @Test
    fun rejectsInvalidDates() {
        assertFailsWith<BackupFormatException> {
            BackupCodec.decode(
                """{"format": "expirywatch-backup", "version": 1,
                   "items": [{"id": 1, "name": "Milk", "expiryDate": "tomorrow"}]}""",
            )
        }
    }

    @Test
    fun rejectsInvalidOrDuplicateIds() {
        // Restoring such ids would break the links between items and their category or location.
        val invalid = listOf(
            """{"format": "expirywatch-backup", "version": 1, "categories": [{"id": 0, "name": "Dairy"}]}""",
            """{"format": "expirywatch-backup", "version": 1, "locations": [{"id": -3, "name": "Fridge"}]}""",
            """{"format": "expirywatch-backup", "version": 1,
               "categories": [{"id": 4, "name": "Dairy"}, {"id": 4, "name": "Meat"}]}""",
            """{"format": "expirywatch-backup", "version": 1,
               "items": [{"id": 0, "name": "Milk", "expiryDate": "2026-03-12"}]}""",
            """{"format": "expirywatch-backup", "version": 2, "shopping": {"lists": [{"id": 0, "name": "Groceries"}]}}""",
            """{"format": "expirywatch-backup", "version": 2,
               "shopping": {"lists": [{"id": 1, "name": "Groceries"}],
                            "items": [{"id": 2, "listId": 1, "name": "Milk"}, {"id": 2, "listId": 1, "name": "Eggs"}]}}""",
        )
        invalid.forEach { json -> assertFailsWith<BackupFormatException>(json) { BackupCodec.decode(json) } }
    }

    @Test
    fun dropsValuesTheEditorWouldReject() {
        val decoded = BackupCodec.decode(
            """
            {"format": "expirywatch-backup", "version": 1,
             "items": [{"id": 1, "name": "Milk", "expiryDate": "2026-03-12", "quantity": 1e12, "useWithinDays": 99999}],
             "products": [{"barcode": "4001234567890", "name": "Milk", "quantity": -2, "useWithinDays": 0}]}
            """.trimIndent(),
        )

        val item = decoded.items.single()
        assertEquals(1.0, item.quantity)
        assertNull(item.useWithinDaysAfterOpening)
        val product = decoded.products.single()
        assertNull(product.quantity)
        assertNull(product.useWithinDaysAfterOpening)
    }

    @Test
    fun toleratesMissingReferencesAndUnknownValues() {
        val decoded = BackupCodec.decode(
            """{"format": "expirywatch-backup", "version": 1, "futureField": true,
               "items": [{"id": 1, "name": "Milk", "expiryDate": "2026-03-12",
                          "categoryId": 99, "groupId": 7, "unit": "BUSHELS", "status": "ACTIVE",
                          "finishedDate": "2026-03-01"}]}""",
        )

        val item = decoded.items.single()
        assertNull(item.category)
        assertNull(item.customGroup)
        assertEquals(QuantityUnit.PIECES, item.unit)
        // Active items never carry a finish date.
        assertNull(item.finishedDate)
    }

    @Test
    fun rejectsShoppingListsAndItemsWithoutNames() {
        val invalid = listOf(
            """{"format": "expirywatch-backup", "version": 2, "shopping": {"lists": [{"id": 1, "name": " "}]}}""",
            """{"format": "expirywatch-backup", "version": 2,
               "shopping": {"lists": [{"id": 1, "name": "Groceries"}], "items": [{"id": 1, "listId": 1, "name": ""}]}}""",
        )
        invalid.forEach { json -> assertFailsWith<BackupFormatException>(json) { BackupCodec.decode(json) } }
    }

    @Test
    fun keepsShoppingListsConsistent() {
        val decoded = BackupCodec.decode(
            """
            {"format": "expirywatch-backup", "version": 2,
             "categories": [{"id": 1, "name": "Dairy"}],
             "shopping": {
               "lists": [{"id": 1, "name": " Groceries "}],
               "items": [
                 {"id": 1, "listId": 1, "name": "Milk", "categoryId": 9, "locationId": 4, "quantity": -1, "unit": "BUSHELS"},
                 {"id": 2, "listId": 5, "name": "Bread"}
               ],
               "history": [
                 {"name": "Milk", "categoryId": 1, "timesAdded": 0},
                 {"name": " MILK", "timesAdded": 7},
                 {"name": "  "}
               ],
               "aisleOrder": [3, 1, 1]
             }}
            """.trimIndent(),
        )

        val shopping = assertNotNull(decoded.shopping)
        assertEquals("Groceries", shopping.lists.single().name)
        // Bread is on a list that isn't in the file, and Milk's category and location aren't either.
        val milk = shopping.items.single()
        assertEquals("Milk", milk.name)
        assertNull(milk.category)
        assertNull(milk.location)
        assertEquals(1.0 to QuantityUnit.PIECES, milk.quantity to milk.unit)
        // A product is remembered once by its name, and as added at least once.
        val remembered = shopping.history.single()
        assertEquals("Milk", remembered.name)
        assertEquals(1L, remembered.category?.id)
        assertEquals(1, remembered.timesAdded)
        assertEquals(listOf(1L), shopping.aisleOrder)
    }
}
