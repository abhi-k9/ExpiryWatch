package io.github.abhik9.expirywatch.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.di.AppDispatcher
import io.github.abhik9.expirywatch.core.common.di.Dispatcher
import io.github.abhik9.expirywatch.core.domain.repository.DocumentStore
import java.io.FileNotFoundException
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
        // "wt" truncates, so overwriting an existing, longer file leaves no stale tail.
        val stream = context.contentResolver.openOutputStream(uri.toUri(), "wt")
            ?: throw IOException("Can't open $uri for writing")
        stream.bufferedWriter().use { it.write(text) }
    }

    override suspend fun readText(uri: String): String = withContext(ioDispatcher) {
        val stream = try {
            context.contentResolver.openInputStream(uri.toUri())
        } catch (e: FileNotFoundException) {
            throw IOException("Can't open $uri", e)
        } ?: throw IOException("Can't open $uri for reading")
        stream.bufferedReader().use { it.readText() }
    }
}
