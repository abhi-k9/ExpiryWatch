package io.github.abhik9.expirywatch.feature.settings

import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.usecase.BackupResult
import io.github.abhik9.expirywatch.core.domain.usecase.ExportBackupUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ExportDiagnosticsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ImportBackupUseCase
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.navigation.LabelKind
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.diagnostics.FakeDiagnosticsRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeBackupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeDocumentStore
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import io.github.abhik9.expirywatch.feature.settings.labels.Label
import io.github.abhik9.expirywatch.feature.settings.labels.ManageLabelsViewModel
import java.io.IOException
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeUserSettingsRepository()
    private val backups = FakeBackupRepository()
    private val documents = FakeDocumentStore()
    private val diagnostics = FakeDiagnosticsRepository(report = "the report")

    // Lazy, so the ViewModel is created after MainDispatcherRule has replaced the main dispatcher.
    private val viewModel by lazy {
        SettingsViewModel(
            settingsRepository = settings,
            exportBackup = ExportBackupUseCase(backups, documents, TestTime.clock, EventLog.NONE),
            importBackup = ImportBackupUseCase(backups, documents, EventLog.NONE),
            diagnosticsRepository = diagnostics,
            exportDiagnostics = ExportDiagnosticsUseCase(diagnostics, documents, EventLog.NONE),
            clock = TestTime.clock,
            appInfo = AppInfo(versionName = "1.0", barcodeEngine = "ZXing", sourceCodeUrl = "https://example.com"),
        )
    }

    @Test
    fun changesAreSaved() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setReminderTime(LocalTime.of(20, 15))
        viewModel.setRemindersEnabled(false)

        assertEquals(ThemeMode.DARK, settings.current.themeMode)
        assertEquals(LocalTime.of(20, 15), settings.current.reminderTime)
        assertFalse(settings.current.remindersEnabled)
    }

    @Test
    fun suggestsADatedBackupFileName() {
        assertEquals("expirywatch-backup-2026-03-10.json", viewModel.suggestedBackupFileName)
    }

    @Test
    fun reportsBackupResults() = runTest {
        viewModel.events.test {
            viewModel.exportTo("content://backup")
            val exported = assertIs<SettingsEvent.BackupFinished>(awaitItem())
            assertEquals(BackupOperation.EXPORT, exported.operation)
            assertIs<BackupResult.Success>(exported.result)

            documents.documents["content://bad"] = "not a backup"
            viewModel.importFrom("content://bad")
            val imported = assertIs<SettingsEvent.BackupFinished>(awaitItem())
            assertIs<BackupResult.InvalidFile>(imported.result)
        }
        assertFalse(viewModel.isBackupInProgress)
    }

    @Test
    fun diagnosticsCanBeRecordedAndCleared() = runTest {
        viewModel.setDiagnosticsRecording(true)
        diagnostics.logSize.value = 2048

        val state = viewModel.diagnostics.first { it.logBytes > 0 }
        assertEquals(DiagnosticsState(isRecording = true, logBytes = 2048), state)

        viewModel.clearDiagnostics()
        assertEquals(0, diagnostics.logSize.value)
    }

    @Test
    fun reportsDiagnosticsExports() = runTest {
        assertEquals("expirywatch-diagnostics-2026-03-10.txt", viewModel.suggestedDiagnosticsFileName)

        viewModel.events.test {
            viewModel.exportDiagnosticsTo("content://diagnostics")
            assertEquals(SettingsEvent.DiagnosticsExported(success = true), awaitItem())

            documents.failWith = IOException("disk full")
            viewModel.exportDiagnosticsTo("content://other")
            assertEquals(SettingsEvent.DiagnosticsExported(success = false), awaitItem())
        }
        assertEquals("the report", documents.documents["content://diagnostics"])
    }

    @Test
    fun labelsCanBeAddedRenamedAndDeleted() = runTest {
        val categories = FakeCategoryRepository(listOf(TestData.dairy))
        val labels = ManageLabelsViewModel(LabelKind.CATEGORIES, categories, FakeStorageLocationRepository())
        backgroundScope.launch { labels.labels.collect {} }

        labels.save(Label(name = "  Spices ", emoji = ""))
        labels.save(Label(id = TestData.dairy.id, name = "Milk products", emoji = "🥛"))
        val afterEdits = labels.labels.filterNotNull().first()
        assertEquals(listOf("Milk products" to "🥛", "Spices" to "📦"), afterEdits.map { it.name to it.emoji })

        labels.delete(afterEdits.first { it.name == "Spices" })
        assertEquals(listOf("Milk products"), labels.labels.filterNotNull().first().map { it.name })
    }
}
