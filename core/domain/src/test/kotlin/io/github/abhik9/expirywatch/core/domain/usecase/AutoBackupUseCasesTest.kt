package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.backup.BackupCodec
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.model.AutoBackupRun
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeAutoBackupScheduler
import io.github.abhik9.expirywatch.core.testing.repository.FakeBackupFolder
import io.github.abhik9.expirywatch.core.testing.repository.FakeBackupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AutoBackupUseCasesTest {
    private val folderUri = "content://tree/backups"
    private val snapshot = BackupSnapshot(
        categories = listOf(TestData.dairy),
        locations = listOf(TestData.fridge),
        items = listOf(TestData.item(id = 1)),
        products = emptyList(),
    )
    private val backups = FakeBackupRepository(snapshot)
    private val folder = FakeBackupFolder()
    private val settings = FakeUserSettingsRepository()
    private val scheduler = FakeAutoBackupScheduler()
    private val runAutoBackup = RunAutoBackupUseCase(backups, folder, settings, TestTime.clock, EventLog.NONE)
    private val setFolder = SetAutoBackupFolderUseCase(settings, folder, scheduler, runAutoBackup)

    @Test
    fun choosingAFolderSchedulesBackupsAndMakesTheFirst() = runTest {
        val result = setFolder(folderUri)

        assertIs<BackupResult.Success>(result)
        assertTrue(scheduler.scheduled)
        assertEquals(setOf(folderUri), folder.accessible)
        val text = folder.folders.getValue(folderUri).getValue("expirywatch-auto-backup-2026-03-10.json")
        assertEquals(snapshot.items, BackupCodec.decode(text).items)
        assertEquals(AutoBackupRun(at = TestTime.now, failed = false), settings.current.lastAutoBackup)
    }

    @Test
    fun keepsTheNewestFiveAutomaticBackupsAndOtherFiles() = runTest {
        settings.setAutoBackupFolder(folderUri)
        val old = (1..6).map { "expirywatch-auto-backup-2026-03-0$it.json" }
        folder.folders[folderUri] = (old + "expirywatch-backup-2026-01-01.json" + "notes.txt")
            .associateWith { "" }
            .toMutableMap()

        runAutoBackup()

        assertEquals(
            listOf(
                "expirywatch-auto-backup-2026-03-03.json",
                "expirywatch-auto-backup-2026-03-04.json",
                "expirywatch-auto-backup-2026-03-05.json",
                "expirywatch-auto-backup-2026-03-06.json",
                "expirywatch-auto-backup-2026-03-10.json",
                "expirywatch-backup-2026-01-01.json",
                "notes.txt",
            ),
            folder.folders.getValue(folderUri).keys.sorted(),
        )
    }

    @Test
    fun aFailedBackupIsRecorded() = runTest {
        settings.setAutoBackupFolder(folderUri)
        folder.failWith = IOException("The folder is gone")

        assertEquals(BackupResult.IoError, runAutoBackup())
        assertEquals(AutoBackupRun(at = TestTime.now, failed = true), settings.current.lastAutoBackup)
    }

    @Test
    fun nothingRunsWhileAutomaticBackupsAreOff() = runTest {
        assertNull(runAutoBackup())
        assertNull(settings.settings.first().lastAutoBackup)
    }

    @Test
    fun turningBackupsOffOrMovingThemGivesUpTheOldFolder() = runTest {
        setFolder(folderUri)
        setFolder("content://tree/elsewhere")
        assertEquals(setOf("content://tree/elsewhere"), folder.accessible)

        assertNull(setFolder(null))

        assertFalse(scheduler.scheduled)
        assertEquals(emptySet(), folder.accessible)
        assertNull(settings.current.autoBackupFolder)
    }
}
