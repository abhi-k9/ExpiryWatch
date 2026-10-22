package io.github.abhik9.expirywatch.core.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.di.AppDispatcher
import io.github.abhik9.expirywatch.core.common.di.Dispatcher
import io.github.abhik9.expirywatch.core.domain.backup.BackupCodec
import io.github.abhik9.expirywatch.core.domain.repository.BackupFolder
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** A folder picked through the Storage Access Framework, as a document tree. */
internal class SafBackupFolder @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : BackupFolder {
    private val resolver get() = context.contentResolver

    override suspend fun keepAccess(folderUri: String) = withContext(ioDispatcher) {
        accessing(folderUri) { resolver.takePersistableUriPermission(folderUri.toUri(), ACCESS) }
    }

    override suspend fun releaseAccess(folderUri: String) = withContext(ioDispatcher) {
        try {
            resolver.releasePersistableUriPermission(folderUri.toUri(), ACCESS)
        } catch (e: SecurityException) {
            // The access was already gone, e.g. revoked by the user.
        }
    }

    override suspend fun write(folderUri: String, fileName: String, text: String) = withContext(ioDispatcher) {
        accessing(folderUri) {
            val tree = folderUri.toUri()
            val document = children(tree)[fileName]
                ?: DocumentsContract.createDocument(resolver, tree.root(), BackupCodec.MIME_TYPE, fileName)
                ?: throw IOException("Can't create $fileName in $folderUri")
            // "wt" truncates, so replacing a longer file leaves no stale tail.
            val stream = resolver.openOutputStream(document, "wt") ?: throw IOException("Can't open $document")
            stream.bufferedWriter().use { it.write(text) }
        }
    }

    override suspend fun fileNames(folderUri: String): List<String> = withContext(ioDispatcher) {
        accessing(folderUri) { children(folderUri.toUri()).keys.toList() }
    }

    override suspend fun delete(folderUri: String, fileName: String) = withContext(ioDispatcher) {
        accessing(folderUri) {
            val document = children(folderUri.toUri())[fileName] ?: return@accessing
            if (!DocumentsContract.deleteDocument(resolver, document)) throw IOException("Can't delete $document")
        }
    }

    /** The folder's files, by name. */
    private fun children(tree: Uri): Map<String, Uri> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val cursor = resolver.query(
            children,
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )
            ?: throw IOException("Can't list $tree")
        return cursor.use {
            buildMap {
                while (it.moveToNext()) put(
                    it.getString(1),
                    DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(0)),
                )
            }
        }
    }

    private fun Uri.root(): Uri = DocumentsContract.buildDocumentUriUsingTree(
        this,
        DocumentsContract.getTreeDocumentId(this),
    )

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

    private companion object {
        const val ACCESS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
