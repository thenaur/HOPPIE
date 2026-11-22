package com.example.hoppie.features.send

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoppie.core.components.FileCategory
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal class SendViewModel(application: Application) : AndroidViewModel(application) {
    private val resolver = application.contentResolver
    private val _state = MutableStateFlow(SendUiState())
    val state = _state.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty() || _state.value.isCheckingFiles) return
        val existingUris = _state.value.files.mapTo(mutableSetOf()) { it.uri }
        val candidates = uris.distinct().filterNot(existingUris::contains)
        if (candidates.isEmpty()) {
            _state.update { it.copy(validationMessages = emptyList()) }
            return
        }

        // OpenMultipleDocuments grants read access for this Activity's task.
        _state.update { it.copy(isCheckingFiles = true, validationMessages = emptyList()) }
        viewModelScope.launch {
            val (accepted, messages) = withContext(Dispatchers.IO) {
                val files = mutableListOf<SelectedFile>()
                val rejected = mutableListOf<String>()
                for (uri in candidates) {
                    coroutineContext.ensureActive()
                    when (val result = readSelectedFile(resolver, uri)) {
                        is FileCheck.Accepted -> files.add(result.file)
                        is FileCheck.Rejected -> rejected.add(result.message)
                    }
                }
                files to rejected
            }
            _state.update {
                it.copy(
                    files = it.files + accepted,
                    validationMessages = messages,
                    isCheckingFiles = false
                )
            }
        }
    }

    fun removeFile(uri: Uri) {
        _state.update { state -> state.copy(files = state.files.filterNot { it.uri == uri }) }
    }
}

private sealed interface FileCheck {
    data class Accepted(val file: SelectedFile) : FileCheck
    data class Rejected(val message: String) : FileCheck
}

private suspend fun readSelectedFile(resolver: ContentResolver, uri: Uri): FileCheck {
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
                coroutineContext.ensureActive()
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
        coroutineContext.ensureActive()
        return unreadable(name)
    }
}

private fun oversized(name: String) =
    FileCheck.Rejected("$name exceeds the 50 MB per-file limit.")

private fun unreadable(name: String) =
    FileCheck.Rejected("$name could not be read. Check that it is available and try again.")
