package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeBackupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeDocumentStore
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
    )
    private val documents = FakeDocumentStore()

    @Test
    fun exportThenImportRestoresTheSameData() = runTest {
        val source = FakeBackupRepository(snapshot)
        val exported = ExportBackupUseCase(source, documents, TestTime.clock, EventLog.NONE)("content://backup")
        assertEquals(BackupResult.Success(BackupStats(itemCount = 2, categoryCount = 1, locationCount = 1)), exported)

        val target = FakeBackupRepository()
        val imported = ImportBackupUseCase(target, documents, EventLog.NONE)("content://backup")

        assertIs<BackupResult.Success>(imported)
        assertEquals(snapshot, target.restoredSnapshot)
    }

    @Test
    fun invalidFilesLeaveDataUntouched() = runTest {
        documents.documents["content://bad"] = "{ nope"
        val target = FakeBackupRepository(snapshot)

        val result = ImportBackupUseCase(target, documents, EventLog.NONE)("content://bad")

        assertIs<BackupResult.InvalidFile>(result)
        assertNull(target.restoredSnapshot)
    }

    @Test
    fun reportsIoErrors() = runTest {
        documents.failWith = IOException("Disk full")

        val exportBackup = ExportBackupUseCase(FakeBackupRepository(), documents, TestTime.clock, EventLog.NONE)
        val importBackup = ImportBackupUseCase(FakeBackupRepository(), documents, EventLog.NONE)

        assertEquals(BackupResult.IoError, exportBackup("x"))
        assertEquals(BackupResult.IoError, importBackup("x"))
    }
}
