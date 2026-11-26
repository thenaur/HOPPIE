package com.example.hoppie.core.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object FileHashUtil {

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).toLowerHex()
    }

    suspend fun computeSha256(openStream: () -> InputStream): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        openStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().toLowerHex()
    }

    suspend fun computeSha256(file: File): String = computeSha256 { file.inputStream() }

    suspend fun computeSha256(context: Context, uri: Uri): String = computeSha256 {
        checkNotNull(context.contentResolver.openInputStream(uri)) {
            "Unable to open stream for $uri"
        }
    }

    suspend fun computeSha256FromPathOrUri(context: Context, pathOrUri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                    computeSha256(context, Uri.parse(pathOrUri))
                } else {
                    val file = File(pathOrUri)
                    if (!file.isFile) return@runCatching null
                    computeSha256(file)
                }
            }.getOrNull()
        }

    fun verifyIntegrity(actualHash: String?, expectedHash: String): Boolean {
        if (actualHash.isNullOrBlank() || expectedHash.isBlank()) return false
        return MessageDigest.isEqual(
            actualHash.trim().lowercase().toByteArray(Charsets.UTF_8),
            expectedHash.trim().lowercase().toByteArray(Charsets.UTF_8)
        )
    }

    private fun ByteArray.toLowerHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
}
