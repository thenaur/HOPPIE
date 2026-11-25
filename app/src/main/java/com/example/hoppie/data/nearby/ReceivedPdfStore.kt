package com.example.hoppie.data.nearby

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.hoppie.core.components.OfferedFile
import com.example.hoppie.core.components.sha256
import com.example.hoppie.features.receive.StoredFile
import com.google.android.gms.nearby.connection.Payload
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class ReceivedPdfStore(private val app: Context) {
    private val resolver get() = app.contentResolver
    private val folder = "${Environment.DIRECTORY_DOWNLOADS}/HOPPIE/"
    private val legacyFolder get() = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: app.filesDir, "HOPPIE")

    suspend fun inspect(files: List<OfferedFile>): List<StoredFile> = withContext(Dispatchers.IO) {
        val sizes = files.mapTo(mutableSetOf()) { it.sizeBytes }
        if (Build.VERSION.SDK_INT < 29) return@withContext legacyFolder.listFiles().orEmpty().filter { it.isFile }.map {
            StoredFile(it.name, it.length(), if (it.length() in sizes) sha256(it::inputStream) else null)
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val columns = arrayOf("_id", "_display_name", "_size")
        buildList {
            resolver.query(collection, columns, "relative_path=? AND is_pending=0", arrayOf(folder), null)?.use { rows ->
                while (rows.moveToNext()) {
                    val uri = ContentUris.withAppendedId(collection, rows.getLong(0))
                    val size = rows.getLong(2)
                    val hash = if (size in sizes) sha256 { checkNotNull(resolver.openInputStream(uri)) } else null
                    add(StoredFile(rows.getString(1), size, hash))
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun open(file: Payload.File): InputStream = if (Build.VERSION.SDK_INT >= 29) {
        checkNotNull(resolver.openInputStream(checkNotNull(file.asUri())))
    } else checkNotNull(file.asJavaFile()).inputStream()

    @Suppress("DEPRECATION")
    fun discardTemporary(payload: Payload) {
        val file = payload.asFile()
        try {
            if (Build.VERSION.SDK_INT >= 29) file?.asUri()?.let { resolver.delete(it, null, null) }
            else file?.asJavaFile()?.delete()
        } catch (_: SecurityException) {
            // Some providers allow reading the Nearby temporary URI but own its cleanup.
        } finally {
            payload.close()
        }
    }

    suspend fun save(fileInfo: OfferedFile, name: String, canPublish: () -> Boolean, open: () -> InputStream): Uri = withContext(Dispatchers.IO) {
        require(TransferFrame.safeName(name))
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put("_display_name", name)
                put("mime_type", fileInfo.mimeType ?: "application/octet-stream")
                put("relative_path", folder)
                put("is_pending", 1)
            }
            val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            var published = false
            try {
                checkNotNull(resolver.openOutputStream(uri)).use { copyExact(open, it, fileInfo.sizeBytes) }
                check(sha256 { checkNotNull(resolver.openInputStream(uri)) } == fileInfo.sha256)
                currentCoroutineContext().ensureActive()
                check(canPublish())
                check(resolver.update(uri, ContentValues().apply { put("is_pending", 0) }, null, null) == 1)
                published = true
                uri
            } finally {
                if (!published) resolver.delete(uri, null, null)
            }
        } else {
            legacyFolder.mkdirs()
            val file = File(legacyFolder, name)
            check(file.createNewFile()) // Reserve exclusively before writing.
            var verified = false
            try {
                file.outputStream().use { copyExact(open, it, fileInfo.sizeBytes) }
                check(sha256(file::inputStream) == fileInfo.sha256)
                check(canPublish())
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.received", file)
                verified = true
                uri
            } finally {
                if (!verified) file.delete()
            }
        }
    }

    private suspend fun copyExact(open: () -> InputStream, out: OutputStream, expected: Long) {
        open().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var count = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = input.read(buffer)
                if (n < 0) break
                count += n
                check(count <= expected)
                out.write(buffer, 0, n)
            }
            check(count == expected)
        }
    }
}
