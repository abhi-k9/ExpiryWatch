package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.backup.BackupCodec
import io.github.abhik9.expirywatch.core.domain.backup.BackupFormatException
import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.domain.repository.BackupRepository
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

data class BackupStats(
    val itemCount: Int,
    val categoryCount: Int,
    val locationCount: Int,
)

sealed interface BackupResult {
    data class Success(val stats: BackupStats) : BackupResult

    /** The document couldn't be read or written. */
    data object IoError : BackupResult

    /** The document isn't a backup this version of the app can read. */
    data class InvalidFile(val reason: String) : BackupResult
}

/** Writes all of the user's data to the document at [uri][invoke]. */
class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val documentStore: DocumentStore,
    private val clock: Clock,
    private val log: EventLog,
) {
    suspend operator fun invoke(uri: String): BackupResult = try {
        val snapshot = backupRepository.createSnapshot()
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
)
