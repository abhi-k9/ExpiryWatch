package io.github.abhik9.expirywatch.core.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * The diagnostics log, written only while recording is on (by default only in debug builds).
 *
 * - Local: kept in the app's no-backup storage, it only leaves the device when the user exports it.
 * - Bounded: rotated into a single previous file once the current one reaches [maxFileBytes].
 * - Cheap when off: messages aren't even built.
 * - Never blocks: lines are written in order on a background thread.
 */
@Singleton
class DiagnosticsLog internal constructor(
    context: Context,
    private val clock: Clock,
    private val writer: Executor,
    private val maxFileBytes: Long,
) : EventLog {
    @Inject
    constructor(@ApplicationContext context: Context, clock: Clock) : this(
        context = context,
        clock = clock,
        writer = Executors.newSingleThreadExecutor { Thread(it, "diagnostics").apply { isDaemon = true } },
        maxFileBytes = MAX_FILE_BYTES,
    )

    // SharedPreferences rather than DataStore: whether to record must be known synchronously,
    // including while the app is crashing.
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dir = File(context.noBackupFilesDir, "diagnostics")
    private val current = File(dir, "diagnostics.log")
    private val previous = File(dir, "diagnostics.1.log")
    private val lock = Any()

    /** Runs after the lines already queued, so reads and clears see every earlier [record]. */
    private val writerDispatcher = writer.asCoroutineDispatcher()

    private val _isRecording = MutableStateFlow(preferences.getBoolean(KEY_RECORDING, context.isDebuggable))
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _size = MutableStateFlow(storedSize())

    /** Size of the recorded log, in bytes. */
    val size: StateFlow<Long> = _size.asStateFlow()

    override fun record(message: () -> String) {
        if (!_isRecording.value) return
        val line = line(message)
        writer.execute { append(line) }
    }

    /** Records synchronously, for when the process is about to die. */
    fun recordNow(message: () -> String) {
        if (!_isRecording.value) return
        append(line(message))
    }

    fun setRecording(enabled: Boolean) {
        if (enabled == _isRecording.value) return
        // Recorded while recording, so that both ends of a recording session are in the log.
        if (enabled) saveRecording(true)
        record { "diagnostics: recording ${if (enabled) "started" else "stopped"}" }
        if (!enabled) saveRecording(false)
    }

    /** Records uncaught exceptions, then lets the app crash as it otherwise would. */
    fun recordCrashes() {
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Never let diagnostics get in the way of the regular crash handling.
            runCatching { recordNow { "crash in ${thread.name}: ${error.stackTraceToString()}" } }
            default?.uncaughtException(thread, error)
        }
    }

    /** The recorded lines, oldest first. */
    suspend fun read(): String = withContext(writerDispatcher) {
        synchronized(lock) {
            listOf(previous, current).filter { it.isFile }.joinToString(separator = "") { it.readText() }
        }
    }

    suspend fun clear() = withContext(writerDispatcher) {
        synchronized(lock) {
            previous.delete()
            current.delete()
            _size.value = storedSize()
        }
    }

    private fun saveRecording(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_RECORDING, enabled) }
        _isRecording.value = enabled
    }

    private fun line(message: () -> String): String {
        val text = try {
            message()
        } catch (e: RuntimeException) {
            "<message failed: $e>"
        }
        val now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS)
        return "$now [${Thread.currentThread().name}] $text\n"
    }

    private fun append(line: String) = synchronized(lock) {
        try {
            dir.mkdirs()
            if (current.length() >= maxFileBytes) {
                previous.delete()
                current.renameTo(previous)
            }
            current.appendText(line)
        } catch (e: IOException) {
            Log.w(TAG, "Couldn't record diagnostics", e)
        }
        _size.value = storedSize()
    }

    private fun storedSize(): Long = current.length() + previous.length()

    private companion object {
        const val TAG = "ExpiryWatch"
        const val PREFERENCES_NAME = "diagnostics"
        const val KEY_RECORDING = "recording"
        const val MAX_FILE_BYTES = 256 * 1024L
    }
}

internal val Context.isDebuggable: Boolean
    get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
