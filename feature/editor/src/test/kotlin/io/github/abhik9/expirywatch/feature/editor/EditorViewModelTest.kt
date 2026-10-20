package io.github.abhik9.expirywatch.feature.editor

import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.shopping.AddItemsToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.LookupProductUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveLabelsByUseUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemUseCase
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.navigation.EditorNavKey
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeCustomGroupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductCatalog
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductHistoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import io.github.abhik9.expirywatch.core.ui.parseQuantity
import io.github.abhik9.expirywatch.core.ui.toQuantityText
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = FakeItemRepository()
    private val history = FakeProductHistoryRepository()
    private val catalog = FakeProductCatalog()
    private val settings = FakeUserSettingsRepository()
    private val shopping = FakeShoppingRepository()
    private val categories = FakeCategoryRepository(listOf(TestData.dairy, TestData.produce))
    private val locations = FakeStorageLocationRepository(listOf(TestData.fridge))
    private val groups = FakeCustomGroupRepository()

    private fun viewModel(key: EditorNavKey = EditorNavKey()) = EditorViewModel(
        key = key,
        categoryRepository = categories,
        locationRepository = locations,
        groupRepository = groups,
        observeLabelsByUse = ObserveLabelsByUseUseCase(categories, locations, groups, items, TestTime.clock),
        itemRepository = items,
        saveItem = SaveItemUseCase(items, history, TestTime.clock),
        finishItem = FinishItemUseCase(items, TestTime.clock),
        lookupProduct = LookupProductUseCase(history, catalog, settings, EventLog.NONE),
        addItemsToShoppingList = AddItemsToShoppingListUseCase(
            GetShoppingListUseCase(shopping, settings, "Shopping list", TestTime.clock),
            AddToShoppingListUseCase(
                shopping,
                ObserveKnownProductsUseCase(shopping, items, TestTime.clock),
                TestTime.clock,
            ),
        ),
        clock = TestTime.clock,
    )

    @Test
    fun requiresNameAndExpiryDate() = runTest {
        val viewModel = viewModel()

        viewModel.save()

        assertEquals(setOf(EditorError.NAME_REQUIRED, EditorError.EXPIRY_REQUIRED), viewModel.errors)
        assertTrue(items.currentItems.isEmpty())
    }

    @Test
    fun savesANewItemAndCloses() = runTest {
        val viewModel = viewModel()
        viewModel.updateForm {
            it.copy(
                name = "Milk",
                categoryId = TestData.dairy.id,
                quantityText = "1,5",
                unit = QuantityUnit.LITERS,
                expiryDate = TestTime.today.plusDays(4),
            )
        }
        assertTrue(viewModel.hasUnsavedChanges)

        viewModel.events.test {
            viewModel.save()
            assertEquals(EditorEvent.Done, awaitItem())
        }

        val saved = items.currentItems.single()
        assertEquals("Milk", saved.name)
        assertEquals(1.5, saved.quantity)
        assertEquals(TestData.dairy, saved.category)
        assertEquals(TestTime.today.plusDays(4), saved.expiryDate)
    }

    @Test
    fun makesCategoriesLocationsAndGroupsWhileEditing() = runTest {
        val viewModel = viewModel()
        viewModel.updateForm { it.copy(name = "Beans", expiryDate = TestTime.today.plusDays(300)) }

        viewModel.addCategory(" Tins ", "🥫")
        viewModel.addLocation("Garage", " ")
        viewModel.addGroup("Camping kit", "⛺")
        viewModel.addGroup("  ", "⛺")

        val tins = categories.observeCategories().first().single { it.name == "Tins" }
        val garage = locations.observeLocations().first().single { it.name == "Garage" }
        val camping = groups.currentGroups.single()
        assertEquals(
            Triple(tins.id, garage.id, camping.id),
            Triple(viewModel.form.categoryId, viewModel.form.locationId, viewModel.form.groupId),
        )
        assertEquals("📦", garage.emoji)

        viewModel.events.test {
            viewModel.save()
            assertEquals(EditorEvent.Done, awaitItem())
        }
        val beans = items.currentItems.single()
        assertEquals(Triple(tins, garage, camping), Triple(beans.category, beans.location, beans.customGroup))
    }

    @Test
    fun savesEachAdditionalDateAsAnItemOfItsOwn() = runTest {
        val viewModel = viewModel()
        viewModel.updateForm {
            it.copy(name = "Yogurt", quantityText = "2", expiryDate = TestTime.today.plusDays(2))
                .withAdditionalDate(TestTime.today.plusDays(5))
                .withAdditionalDate(TestTime.today.plusDays(9))
                .withAdditionalDate(TestTime.today.plusDays(12))
        }
        val (_, second, third) = viewModel.form.additionalDates
        viewModel.updateForm { it.updateAdditionalDate(second.key) { row -> row.copy(quantityText = "3") } }
        viewModel.updateForm { it.withoutAdditionalDate(third.key) }

        viewModel.events.test {
            viewModel.save()
            assertEquals(EditorEvent.Done, awaitItem())
        }

        val today = TestTime.today
        assertEquals(
            listOf(today.plusDays(2) to 2.0, today.plusDays(5) to 1.0, today.plusDays(9) to 3.0),
            items.currentItems.map { it.expiryDate to it.quantity },
        )
        assertTrue(items.currentItems.all { it.name == "Yogurt" })
    }

    @Test
    fun anInvalidAdditionalQuantityIsShownAndNothingIsSaved() = runTest {
        val viewModel = viewModel()
        viewModel.updateForm {
            it.copy(name = "Yogurt", expiryDate = TestTime.today).withAdditionalDate(TestTime.today.plusDays(3))
        }
        val key = viewModel.form.additionalDates.single().key
        viewModel.updateForm { it.updateAdditionalDate(key) { row -> row.copy(quantityText = "0") } }

        viewModel.save()

        assertEquals(setOf(key), viewModel.invalidAdditionalQuantities)
        assertEquals(emptySet(), viewModel.errors)
        assertTrue(items.currentItems.isEmpty())

        // Fixing it clears the error straight away.
        viewModel.updateForm { it.updateAdditionalDate(key) { row -> row.copy(quantityText = "1") } }
        assertEquals(emptySet(), viewModel.invalidAdditionalQuantities)
    }

    @Test
    fun scanningFillsEmptyFieldsFromTheCatalog() = runTest {
        catalog.products["3017620422003"] = ProductInfo(
            barcode = "3017620422003",
            name = "Nutella",
            brand = "Ferrero",
            imageUrl = "https://example.com/nutella.jpg",
            source = ProductSource.OPEN_FOOD_FACTS,
        )
        val viewModel = viewModel()
        viewModel.updateForm { it.copy(brand = "My brand") }

        viewModel.onBarcodeScanned("3017620422003")

        assertEquals(LookupState.Found(ProductSource.OPEN_FOOD_FACTS), viewModel.lookup)
        assertEquals("Nutella", viewModel.form.name)
        // What the user typed is kept.
        assertEquals("My brand", viewModel.form.brand)
        assertEquals("https://example.com/nutella.jpg", viewModel.form.imageUrl)
    }

    @Test
    fun reportsAnUnreachableCatalog() = runTest {
        catalog.isOffline = true
        val viewModel = viewModel()

        viewModel.onBarcodeScanned("3017620422003")

        assertEquals(LookupState.Unavailable, viewModel.lookup)
    }

    @Test
    fun saysWhenOnlineLookupIsOff() = runTest {
        settings.setOnlineProductLookup(false)
        val viewModel = viewModel()

        viewModel.onBarcodeScanned("3017620422003")

        assertEquals(LookupState.NotFoundOnlineLookupOff, viewModel.lookup)
        assertEquals(emptyList(), catalog.requestedBarcodes)
    }

    @Test
    fun editingLoadsTheItemAndFinishingCloses() = runTest {
        items.setItems(listOf(TestData.item(id = 5, name = "Butter").copy(openedDate = TestTime.today)))
        val viewModel = viewModel(EditorNavKey(itemId = 5))

        assertFalse(viewModel.isLoading)
        assertEquals("Butter", viewModel.form.name)
        assertTrue(viewModel.form.isOpened)
        assertFalse(viewModel.hasUnsavedChanges)

        viewModel.events.test {
            viewModel.finish(ItemStatus.WASTED)
            assertEquals(EditorEvent.Done, awaitItem())
        }
        assertEquals(ItemStatus.WASTED, items.currentItems.single().status)
    }

    @Test
    fun putsTheItemOnTheShoppingList() = runTest {
        items.upsert(TestData.item(id = 5, name = "Milk"))
        val viewModel = viewModel(EditorNavKey(itemId = 5))

        viewModel.events.test {
            viewModel.addToShoppingList()

            val added = assertIs<EditorEvent.PutOnShoppingList>(awaitItem()).added
            assertEquals(listOf("Milk"), added.names)
        }
        assertEquals(listOf("Milk"), shopping.currentItems.map { it.name })
    }

    @Test
    fun closesWhenTheItemNoLongerExists() = runTest {
        val viewModel = viewModel(EditorNavKey(itemId = 99))

        viewModel.events.test {
            assertEquals(EditorEvent.Done, awaitItem())
        }
        assertNull(items.currentItems.firstOrNull())
    }

    @Test
    fun parsesQuantitiesWithDecimalCommaOrPoint() {
        assertEquals(1.5, "1,5".parseQuantity())
        assertEquals(2.0, " 2 ".parseQuantity())
        assertNull("two".parseQuantity())
    }

    @Test
    fun formattedQuantitiesCanBeParsedBackInEveryLocale() {
        val locales = listOf("en-US", "de-DE", "fr-CH", "ar-EG-u-nu-arab", "fa-IR", "hi-IN-u-nu-deva")
        locales.map(Locale::forLanguageTag).forEach { locale ->
            listOf(1.0, 1.5, 0.25, 1234.5).forEach { quantity ->
                assertEquals(quantity, quantity.toQuantityText(locale).parseQuantity(), "$quantity in $locale")
            }
        }
        assertEquals("1,5", 1.5.toQuantityText(Locale.GERMANY))
    }
}
