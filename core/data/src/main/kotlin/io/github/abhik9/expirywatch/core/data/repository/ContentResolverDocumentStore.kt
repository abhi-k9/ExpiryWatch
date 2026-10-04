package io.github.abhik9.expirywatch.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.di.AppDispatcher
import io.github.abhik9.expirywatch.core.common.di.Dispatcher
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Reads and writes documents chosen through the Storage Access Framework. */
internal class ContentResolverDocumentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DocumentStore {
    override suspend fun writeText(uri: String, text: String) = withContext(ioDispatcher) {
        accessing(uri) {
            // "wt" truncates, so overwriting an existing, longer file leaves no stale tail.
            val stream = context.contentResolver.openOutputStream(uri.toUri(), "wt")
                ?: throw IOException("Can't open $uri for writing")
            stream.bufferedWriter().use { it.write(text) }
        }
    }

    override suspend fun readText(uri: String): String = withContext(ioDispatcher) {
        accessing(uri) {
            val stream = context.contentResolver.openInputStream(uri.toUri())
                ?: throw IOException("Can't open $uri for reading")
            stream.bufferedReader().use { it.readText() }
        }
    }

    /**
     * Document providers report failures in different ways, for example a revoked permission as a
     * SecurityException, so all of them become the IOException callers expect.
     */
    private inline fun <T> accessing(uri: String, block: () -> T): T = try {
        block()
    } catch (e: IOException) {
        throw e
    } catch (e: RuntimeException) {
        throw IOException("Can't access $uri", e)
    }
}
