package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.backup.BackupCodec
import io.github.abhik9.expirywatch.core.domain.backup.BackupFormatException
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.domain.repository.AutoBackupScheduler
import io.github.abhik9.expirywatch.core.domain.repository.BackupFolder
import io.github.abhik9.expirywatch.core.domain.repository.BackupRepository
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.AutoBackupRun
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

data class BackupStats(
    val itemCount: Int,
    val categoryCount: Int,
    val locationCount: Int,
    val shoppingListCount: Int = 0,
    val shoppingItemCount: Int = 0,
)

sealed interface BackupResult {
    data class Success(val stats: BackupStats) : BackupResult

    /** The document couldn't be read or written. */
    data object IoError : BackupResult

    /** The document isn't a backup this version of the app can read. */
    data class InvalidFile(val reason: String) : BackupResult
}

/** Reads everything the user has entered, along with the settings that belong in a backup. */
private suspend fun BackupRepository.createSnapshot(settingsRepository: UserSettingsRepository): BackupSnapshot {
    val aisleOrder = settingsRepository.settings.first().shoppingAisleOrder
    return createSnapshot().let { it.copy(shopping = it.shopping?.copy(aisleOrder = aisleOrder)) }
}

/** Writes all of the user's data to the document at [uri][invoke]. */
class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val documentStore: DocumentStore,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
    private val log: EventLog,
) {
    suspend operator fun invoke(uri: String): BackupResult = try {
        val snapshot = backupRepository.createSnapshot(settingsRepository)
        documentStore.writeText(uri, BackupCodec.encode(snapshot, exportedAt = Instant.now(clock)))
        BackupResult.Success(snapshot.stats()).also { log.record { "backup: exported ${it.stats}" } }
    } catch (e: IOException) {
        log.record { "backup: export failed: $e" }
        BackupResult.IoError
    }
}

/**
 * Replaces all of the user's data with the backup at [uri][invoke]. The file is fully read and
 * validated before anything is changed, so a bad file leaves the current data untouched.
 */
class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val documentStore: DocumentStore,
    private val settingsRepository: UserSettingsRepository,
    private val log: EventLog,
) {
    suspend operator fun invoke(uri: String): BackupResult {
        val snapshot = try {
            BackupCodec.decode(documentStore.readText(uri))
        } catch (e: IOException) {
            log.record { "backup: import failed: $e" }
            return BackupResult.IoError
        } catch (e: BackupFormatException) {
            log.record { "backup: import rejected: ${e.message}" }
            return BackupResult.InvalidFile(e.message.orEmpty())
        }
        try {
            backupRepository.restore(snapshot)
            snapshot.shopping?.let { settingsRepository.setShoppingAisleOrder(it.aisleOrder) }
        } catch (e: IOException) {
            log.record { "backup: restore failed: $e" }
            return BackupResult.IoError
        }
        return BackupResult.Success(snapshot.stats()).also { log.record { "backup: imported ${it.stats}" } }
    }
}

private fun BackupSnapshot.stats() = BackupStats(
    itemCount = items.size,
    categoryCount = categories.size,
    locationCount = locations.size,
    shoppingListCount = shopping?.lists?.size ?: 0,
    shoppingItemCount = shopping?.items?.size ?: 0,
)

/**
 * Backs up all of the user's data to the folder they picked, as a dated file that replaces one
 * from the same day, and deletes all but the [newest few][KEEP_AUTO_BACKUPS] such files. Records
 * how it went in the settings. Does nothing while automatic backups are off.
 */
class RunAutoBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val backupFolder: BackupFolder,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
    private val log: EventLog,
) {
    /** Returns the result, or `null` while automatic backups are off. */
    suspend operator fun invoke(): BackupResult? {
        val folder = settingsRepository.settings.first().autoBackupFolder ?: return null
        val now = Instant.now(clock)
        val result = try {
            val snapshot = backupRepository.createSnapshot(settingsRepository)
            backupFolder.write(folder, autoBackupFileName(LocalDate.now(clock)), BackupCodec.encode(snapshot, now))
            backupFolder.fileNames(folder)
                .filter { AUTO_BACKUP_FILE_NAME.matches(it) }
                // The dates in the names sort them oldest first.
                .sortedDescending()
                .drop(KEEP_AUTO_BACKUPS)
                .forEach { backupFolder.delete(folder, it) }
            BackupResult.Success(snapshot.stats()).also { log.record { "backup: automatic backup done, ${it.stats}" } }
        } catch (e: IOException) {
            log.record { "backup: automatic backup failed: $e" }
            BackupResult.IoError
        }
        settingsRepository.setLastAutoBackup(AutoBackupRun(at = now, failed = result !is BackupResult.Success))
        return result
    }

    companion object {
        /** How many automatic backups are kept in the folder. */
        const val KEEP_AUTO_BACKUPS = 5

        private val AUTO_BACKUP_FILE_NAME = Regex("""expirywatch-auto-backup-\d{4}-\d{2}-\d{2}\.json""")

        fun autoBackupFileName(date: LocalDate) = "expirywatch-auto-backup-$date.json"
    }
}

/**
 * Starts automatic weekly backups to the folder at [uri][invoke], with one right away, or stops
 * them when it's `null`.
 */
class SetAutoBackupFolderUseCase @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val backupFolder: BackupFolder,
    private val scheduler: AutoBackupScheduler,
    private val runAutoBackup: RunAutoBackupUseCase,
) {
    /** Returns the result of the first backup, or `null` when turning them off. */
    suspend operator fun invoke(uri: String?): BackupResult? {
        val previous = settingsRepository.settings.first().autoBackupFolder
        if (uri != null) {
            try {
                backupFolder.keepAccess(uri)
            } catch (e: IOException) {
                return BackupResult.IoError
            }
        }
        settingsRepository.setAutoBackupFolder(uri)
        if (previous != null && previous != uri) backupFolder.releaseAccess(previous)
        if (uri == null) {
            scheduler.cancel()
            return null
        }
        scheduler.scheduleWeekly()
        return runAutoBackup()
    }
}
