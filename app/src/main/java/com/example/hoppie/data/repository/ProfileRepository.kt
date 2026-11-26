package com.example.hoppie.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.hoppie.data.local.ProfilePreferences
import com.example.hoppie.data.local.ProfilePreferencesStore
import java.io.File
import java.text.DecimalFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class StorageUsageBreakdown(
    val receivedFilesBytes: Long = 0L,
    val receivedFileCount: Int = 0,
    val previewCacheBytes: Long = 0L
) {
    val totalUsedBytes: Long
        get() = receivedFilesBytes + previewCacheBytes
}

object ProfileStorageFormatter {
    fun formatBytes(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        if (safeBytes < 1024L) return "$safeBytes B"
        val kb = safeBytes / 1024.0
        if (kb < 1024.0) return "${formatNumber(kb)} KB"
        val mb = kb / 1024.0
        if (mb < 1024.0) return "${formatNumber(mb)} MB"
        val gb = mb / 1024.0
        return "${formatNumber(gb)} GB"
    }

    private fun formatNumber(value: Double): String =
        DecimalFormat("#.#").format(value).removeSuffix(".0")
}

class ProfileRepository(
    context: Context,
    private val preferencesStore: ProfilePreferencesStore
) {
    private val appContext = context.applicationContext

    val preferences: StateFlow<ProfilePreferences> = preferencesStore.preferences

    fun updateDeviceName(name: String): Boolean = preferencesStore.updateDeviceName(name)

    fun updateSaveLocation(path: String): Boolean = preferencesStore.updateSaveLocation(path)

    fun resetSaveLocation() = preferencesStore.resetSaveLocation()

    fun setAutoSkipDuplicates(enabled: Boolean) = preferencesStore.setAutoSkipDuplicates(enabled)

    fun setAutoCachePreviews(enabled: Boolean) = preferencesStore.setAutoCachePreviews(enabled)

    suspend fun calculateStorageUsage(): StorageUsageBreakdown = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        var fileCount = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads.SIZE)
            val selection =
                "(${MediaStore.Downloads.RELATIVE_PATH}=? OR ${MediaStore.Downloads.RELATIVE_PATH}=?) AND ${MediaStore.Downloads.IS_PENDING}=0"
            val args = arrayOf(
                "${Environment.DIRECTORY_DOWNLOADS}/HOPPIE/",
                "${Environment.DIRECTORY_DOWNLOADS}/Hoppie/"
            )
            runCatching {
                appContext.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    while (cursor.moveToNext()) {
                        totalBytes += cursor.getLong(sizeCol).coerceAtLeast(0L)
                        fileCount++
                    }
                }
            }
        }

        val candidateDirs = listOfNotNull(
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "HOPPIE") },
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "Hoppie") },
            File(appContext.filesDir, "HOPPIE")
        ).distinctBy { it.absolutePath }

        candidateDirs.forEach { dir ->
            if (dir.isDirectory) {
                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                    totalBytes += file.length()
                    fileCount++
                }
            }
        }

        val cacheDir = previewCacheDir()
        val previewCacheBytes = if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }

        StorageUsageBreakdown(
            receivedFilesBytes = totalBytes,
            receivedFileCount = fileCount,
            previewCacheBytes = previewCacheBytes
        )
    }

    suspend fun clearPreviewCache(): Long = withContext(Dispatchers.IO) {
        val cacheDir = previewCacheDir()
        if (!cacheDir.exists()) return@withContext 0L
        val cleared = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        cacheDir.deleteRecursively()
        cleared
    }

    private fun previewCacheDir(): File = File(appContext.cacheDir, PREVIEW_CACHE_DIR_NAME)

    companion object {
        private const val PREVIEW_CACHE_DIR_NAME = "preview_cache"
    }
}
