package com.example.hoppie.features.send

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.hoppie.core.components.FileCategory
import java.io.IOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal sealed interface FileCheck {
    data class Accepted(val file: SelectedFile) : FileCheck
    data class Rejected(val message: String) : FileCheck
}

internal suspend fun readSelectedFile(resolver: ContentResolver, uri: Uri): FileCheck {
    var name = "Selected file"
    try {
        val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val declaredSize = resolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { column ->
                cursor.getString(column)?.takeIf(String::isNotBlank)?.let { name = it }
            }
            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeColumn < 0 || cursor.isNull(sizeColumn)) null
            else cursor.getString(sizeColumn)?.toLongOrNull()?.takeIf { it >= 0 }
        }
        if (declaredSize != null && declaredSize > MAX_FILE_BYTES) return oversized(name)

        val mimeType = resolver.getType(uri)
        val stream = resolver.openInputStream(uri) ?: throw IOException("Missing document stream")
        val size = stream.use { input ->
            if (declaredSize != null) return@use declaredSize

            // Stop after the first byte over the limit and keep memory usage bounded.
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead = 0L
            while (bytesRead <= MAX_FILE_BYTES) {
                currentCoroutineContext().ensureActive()
                val readLimit = minOf(buffer.size.toLong(), MAX_FILE_BYTES + 1 - bytesRead)
                val count = input.read(buffer, 0, readLimit.toInt())
                if (count < 0) break
                bytesRead += count
            }
            bytesRead
        }
        if (size > MAX_FILE_BYTES) return oversized(name)

        return FileCheck.Accepted(
            SelectedFile(uri, name, mimeType, size, FileCategory.from(name, mimeType))
        )
    } catch (_: IOException) {
        return unreadable(name)
    } catch (_: RuntimeException) {
        currentCoroutineContext().ensureActive()
        return unreadable(name)
    }
}

private fun oversized(name: String) =
    FileCheck.Rejected("$name exceeds the 50 MB per-file limit.")

private fun unreadable(name: String) =
    FileCheck.Rejected("$name could not be read. Check that it is available and try again.")
