package io.github.abhik9.expirywatch.core.testing.repository

import io.github.abhik9.expirywatch.core.domain.backup.BackupSnapshot
import io.github.abhik9.expirywatch.core.domain.repository.BackupRepository
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
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
    var scheduleCalls = 0
        private set

    override suspend fun scheduleDaily(time: LocalTime) {
        scheduledTime = time
        scheduleCalls++
    }

    override suspend fun cancel() {
        scheduledTime = null
    }
}
