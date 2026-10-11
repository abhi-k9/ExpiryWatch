package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.domain.backup.ShoppingBackup
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeBackupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeDocumentStore
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class BackupUseCasesTest {
    private val snapshot = BackupSnapshot(
        categories = listOf(TestData.dairy),
        locations = listOf(TestData.fridge),
        items = listOf(TestData.item(id = 1), TestData.item(id = 2, name = "Butter")),
        products = emptyList(),
        shopping = ShoppingBackup(
            lists = listOf(ShoppingList(id = 1, name = "Groceries", createdAt = TestTime.now)),
            items = listOf(TestData.shoppingItem(id = 1, name = "Eggs")),
        ),
    )
    private val documents = FakeDocumentStore()
    private val settings = FakeUserSettingsRepository()

    @Test
    fun exportThenImportRestoresTheSameData() = runTest {
        val source = FakeBackupRepository(snapshot)
        val sourceSettings = FakeUserSettingsRepository(UserSettings(shoppingAisleOrder = listOf(1L)))
        val exportBackup = ExportBackupUseCase(source, documents, sourceSettings, TestTime.clock, EventLog.NONE)
        val exported = exportBackup("content://backup")
        assertEquals(
            BackupResult.Success(
                BackupStats(
                    itemCount = 2,
                    categoryCount = 1,
                    locationCount = 1,
                    shoppingListCount = 1,
                    shoppingItemCount = 1,
                ),
            ),
            exported,
        )

        val target = FakeBackupRepository()
        val imported = ImportBackupUseCase(target, documents, settings, EventLog.NONE)("content://backup")

        assertIs<BackupResult.Success>(imported)
        assertEquals(
            snapshot.copy(shopping = snapshot.shopping?.copy(aisleOrder = listOf(1L))),
            target.restoredSnapshot,
        )
        // The order of the shop's aisles comes back with the lists.
        assertEquals(listOf(1L), settings.current.shoppingAisleOrder)
    }

    @Test
    fun backupsFromBeforeShoppingListsLeaveTheAislesAsTheyAre() = runTest {
        documents.documents["content://old"] =
            """{"format": "expirywatch-backup", "version": 1, "categories": [{"id": 1, "name": "Dairy"}]}"""
        settings.setShoppingAisleOrder(listOf(2L, 1L))
        val target = FakeBackupRepository(snapshot)

        val result = ImportBackupUseCase(target, documents, settings, EventLog.NONE)("content://old")

        assertIs<BackupResult.Success>(result)
        assertNull(target.restoredSnapshot?.shopping)
        assertEquals(listOf(2L, 1L), settings.current.shoppingAisleOrder)
    }

    @Test
    fun invalidFilesLeaveDataUntouched() = runTest {
        documents.documents["content://bad"] = "{ nope"
        val target = FakeBackupRepository(snapshot)

        val result = ImportBackupUseCase(target, documents, settings, EventLog.NONE)("content://bad")

        assertIs<BackupResult.InvalidFile>(result)
        assertNull(target.restoredSnapshot)
    }

    @Test
    fun reportsIoErrors() = runTest {
        documents.failWith = IOException("Disk full")

        val exportBackup =
            ExportBackupUseCase(FakeBackupRepository(), documents, settings, TestTime.clock, EventLog.NONE)
        val importBackup = ImportBackupUseCase(FakeBackupRepository(), documents, settings, EventLog.NONE)

        assertEquals(BackupResult.IoError, exportBackup("x"))
        assertEquals(BackupResult.IoError, importBackup("x"))
    }
}
