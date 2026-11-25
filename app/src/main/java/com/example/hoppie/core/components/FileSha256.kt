package com.example.hoppie.core.components

import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal suspend fun sha256(openStream: () -> InputStream): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256")
    openStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
