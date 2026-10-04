package io.github.abhik9.expirywatch.core.domain.backup

import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupCodecTest {
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
            ),
            TestData.item(id = 2, name = "Spinach", category = TestData.produce, location = null).copy(
                status = ItemStatus.WASTED,
                finishedDate = TestTime.today,
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
    )

    @Test
    fun roundTripsASnapshot() {
        val decoded = BackupCodec.decode(BackupCodec.encode(snapshot, exportedAt = TestTime.now))

        assertEquals(snapshot.categories, decoded.categories)
        assertEquals(snapshot.locations, decoded.locations)
        assertEquals(snapshot.items, decoded.items)
        assertEquals(snapshot.products, decoded.products)
    }

    @Test
    fun writesAVersionedDocument() {
        val json = BackupCodec.encode(snapshot, exportedAt = TestTime.now)

        assertTrue(json.contains("\"format\": \"expirywatch-backup\""))
        assertTrue(json.contains("\"version\": 1"))
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
                          "categoryId": 99, "unit": "BUSHELS", "status": "ACTIVE",
                          "finishedDate": "2026-03-01"}]}""",
        )

        val item = decoded.items.single()
        assertNull(item.category)
        assertEquals(QuantityUnit.PIECES, item.unit)
        // Active items never carry a finish date.
        assertNull(item.finishedDate)
    }
}
