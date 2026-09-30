package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot

interface BackupRepository {
    /** Reads everything the user has entered. */
    suspend fun createSnapshot(): BackupSnapshot

    /** Atomically replaces all of the user's data with [snapshot]. */
    suspend fun restore(snapshot: BackupSnapshot)
}

/** Reads and writes documents the user picked, identified by a content URI string. */
interface DocumentStore {
    /** @throws java.io.IOException when the document can't be written. */
    suspend fun writeText(uri: String, text: String)

    /** @throws java.io.IOException when the document can't be read. */
    suspend fun readText(uri: String): String
}
