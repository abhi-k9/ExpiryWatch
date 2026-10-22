package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot

interface BackupRepository {
    /**
     * Reads everything the user has entered.
     * @throws java.io.IOException when the data can't be read.
     */
    suspend fun createSnapshot(): BackupSnapshot

    /**
     * Atomically replaces all of the user's data with [snapshot]. A snapshot without
     * [shopping lists][BackupSnapshot.shopping] keeps the current ones, linked to the restored
     * categories and locations of the same names.
     * @throws java.io.IOException when the data can't be written; nothing is changed then.
     */
    suspend fun restore(snapshot: BackupSnapshot)
}

/** Reads and writes documents the user picked, identified by a content URI string. */
interface DocumentStore {
    /** @throws java.io.IOException when the document can't be written. */
    suspend fun writeText(uri: String, text: String)

    /** @throws java.io.IOException when the document can't be read. */
    suspend fun readText(uri: String): String
}

/** A folder the user picked to keep backups in, identified by a document tree URI string. */
interface BackupFolder {
    /** Keeps the app's access to the folder at [folderUri] after a restart. */
    suspend fun keepAccess(folderUri: String)

    /** Gives up the app's access to the folder at [folderUri], once it's no longer backed up to. */
    suspend fun releaseAccess(folderUri: String)

    /**
     * Writes [text] to the file named [fileName] in the folder, replacing it if it's there.
     * @throws java.io.IOException when the file can't be written, e.g. the folder is gone.
     */
    suspend fun write(folderUri: String, fileName: String, text: String)

    /** @throws java.io.IOException when the folder can't be read. */
    suspend fun fileNames(folderUri: String): List<String>

    /** @throws java.io.IOException when the file can't be deleted. */
    suspend fun delete(folderUri: String, fileName: String)
}

/** Runs the automatic backup every week while it's on. */
interface AutoBackupScheduler {
    suspend fun scheduleWeekly()

    suspend fun cancel()
}
