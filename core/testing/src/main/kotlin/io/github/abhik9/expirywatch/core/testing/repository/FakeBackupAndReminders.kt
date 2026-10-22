package io.github.abhik9.expirywatch.core.testing.repository

import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.domain.repository.AutoBackupScheduler
import io.github.abhik9.expirywatch.core.domain.repository.BackupFolder
import io.github.abhik9.expirywatch.core.domain.repository.BackupRepository
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
import io.github.abhik9.expirywatch.core.domain.repository.ReminderHealth
import io.github.abhik9.expirywatch.core.domain.repository.ReminderHealthMonitor
import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import java.io.IOException
import java.time.LocalTime

class FakeBackupRepository(
    var snapshot: BackupSnapshot = BackupSnapshot(emptyList(), emptyList(), emptyList(), emptyList()),
) : BackupRepository {
    var restoredSnapshot: BackupSnapshot? = null
        private set

    override suspend fun createSnapshot(): BackupSnapshot = snapshot

    override suspend fun restore(snapshot: BackupSnapshot) {
        restoredSnapshot = snapshot
        this.snapshot = snapshot
    }
}

class FakeDocumentStore : DocumentStore {
    val documents = mutableMapOf<String, String>()
    var failWith: IOException? = null

    override suspend fun writeText(uri: String, text: String) {
        failWith?.let { throw it }
        documents[uri] = text
    }

    override suspend fun readText(uri: String): String {
        failWith?.let { throw it }
        return documents[uri] ?: throw IOException("No document at $uri")
    }
}

class FakeReminderScheduler : ReminderScheduler {
    /** The currently scheduled time, or `null` when no reminder is scheduled. */
    var scheduledTime: LocalTime? = null
        private set

    /** Whether the scheduled reminder is exact. */
    var exact = false
        private set
    var scheduleCalls = 0
        private set

    override suspend fun scheduleDaily(time: LocalTime, exact: Boolean) {
        scheduledTime = time
        this.exact = exact
        scheduleCalls++
    }

    override suspend fun cancel() {
        scheduledTime = null
        exact = false
    }
}

class FakeReminderHealthMonitor(
    var health: ReminderHealth = ReminderHealth(
        notificationsAllowed = true,
        exactAlarmsAllowed = true,
        backgroundRestricted = false,
        lastShown = null,
    ),
) : ReminderHealthMonitor {
    override fun current(): ReminderHealth = health
}

class FakeBackupFolder : BackupFolder {
    /** The files in each folder, by name. */
    val folders = mutableMapOf<String, MutableMap<String, String>>()

    /** The folders the app keeps access to. */
    val accessible = mutableSetOf<String>()
    var failWith: IOException? = null

    override suspend fun keepAccess(folderUri: String) {
        accessible += folderUri
    }

    override suspend fun releaseAccess(folderUri: String) {
        accessible -= folderUri
    }

    override suspend fun write(folderUri: String, fileName: String, text: String) {
        failWith?.let { throw it }
        folders.getOrPut(folderUri) { mutableMapOf() }[fileName] = text
    }

    override suspend fun fileNames(folderUri: String): List<String> {
        failWith?.let { throw it }
        return folders[folderUri].orEmpty().keys.toList()
    }

    override suspend fun delete(folderUri: String, fileName: String) {
        failWith?.let { throw it }
        folders[folderUri]?.remove(fileName)
    }
}

class FakeAutoBackupScheduler : AutoBackupScheduler {
    var scheduled = false
        private set

    override suspend fun scheduleWeekly() {
        scheduled = true
    }

    override suspend fun cancel() {
        scheduled = false
    }
}
