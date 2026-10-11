package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.model.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShoppingEntryTest {
    private fun assertParsed(
        text: String,
        name: String,
        quantity: Double = 1.0,
        unit: QuantityUnit = QuantityUnit.PIECES,
    ) =
        assertEquals(ShoppingEntry(name, quantity, unit), ShoppingEntry.parse(text), "\"$text\"")

    @Test
    fun readsAQuantityAndUnitBeforeTheName() {
        assertParsed("2 milk", "milk", 2.0)
        assertParsed("  2   Oat  milk ", "Oat milk", 2.0)
        assertParsed("1.5 kg flour", "flour", 1.5, QuantityUnit.KILOGRAMS)
        assertParsed("1,5 kg flour", "flour", 1.5, QuantityUnit.KILOGRAMS)
        assertParsed("500g butter", "butter", 500.0, QuantityUnit.GRAMS)
        assertParsed("250 ML cream", "cream", 250.0, QuantityUnit.MILLILITERS)
        assertParsed("2 l. water", "water", 2.0, QuantityUnit.LITERS)
        assertParsed("3 packs of pasta", "pasta", 3.0, QuantityUnit.PACKS)
        assertParsed("2x lemons", "lemons", 2.0)
    }

    @Test
    fun readsACountAfterTheName() {
        assertParsed("eggs x12", "eggs", 12.0)
        assertParsed("eggs x 12", "eggs", 12.0)
        assertParsed("Eggs ×6", "Eggs", 6.0)
    }

    @Test
    fun keepsNumbersThatArePartOfTheName() {
        assertParsed("7up", "7up")
        assertParsed("3D glasses", "3D glasses")
        assertParsed("500g", "500g")
        assertParsed("1/2 lemon", "1/2 lemon")
    }

    @Test
    fun ignoresQuantitiesThatAreTooSmallOrTooLarge() {
        assertParsed("0 milk", "0 milk")
        assertParsed("1000001 grains of rice", "1000001 grains of rice")
    }

    @Test
    fun readsNothingFromBlankText() {
        assertNull(ShoppingEntry.parse(""))
        assertNull(ShoppingEntry.parse("   "))
    }
}
