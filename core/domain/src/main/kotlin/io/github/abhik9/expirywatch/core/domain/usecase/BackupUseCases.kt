package io.github.abhik9.expirywatch.core.domain.usecase

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
) {
    suspend operator fun invoke(uri: String): BackupResult {
        val snapshot = backupRepository.createSnapshot()
        return try {
            documentStore.writeText(uri, BackupCodec.encode(snapshot, exportedAt = Instant.now(clock)))
            BackupResult.Success(snapshot.stats())
        } catch (e: IOException) {
            BackupResult.IoError
        }
    }
}

/**
 * Replaces all of the user's data with the backup at [uri][invoke]. The file is fully read and
 * validated before anything is changed, so a bad file leaves the current data untouched.
 */
class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val documentStore: DocumentStore,
) {
    suspend operator fun invoke(uri: String): BackupResult {
        val snapshot = try {
            BackupCodec.decode(documentStore.readText(uri))
        } catch (e: IOException) {
            return BackupResult.IoError
        } catch (e: BackupFormatException) {
            return BackupResult.InvalidFile(e.message.orEmpty())
        }
        backupRepository.restore(snapshot)
        return BackupResult.Success(snapshot.stats())
    }
}

private fun BackupSnapshot.stats() = BackupStats(
    itemCount = items.size,
    categoryCount = categories.size,
    locationCount = locations.size,
)
