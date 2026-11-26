package com.example.hoppie.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.hoppie.core.utils.FileHashUtil
import com.example.hoppie.core.utils.StorageFormatter
import com.example.hoppie.data.local.dao.ReceivedFileDao
import com.example.hoppie.data.local.entities.ReceivedFileEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class LibrarySortOption(val key: String, val label: String) {
    Newest("NEWEST", "Newest"),
    Oldest("OLDEST", "Oldest"),
    Name("NAME", "Name"),
    Size("SIZE", "Size")
}

sealed interface FileIntegrityResult {
    data class Verified(val computedHash: String) : FileIntegrityResult
    data class Mismatch(val expectedHash: String, val actualHash: String) : FileIntegrityResult
    data object FileUnavailable : FileIntegrityResult
}

fun interface FileRedistributionHook {
    fun onRedistribute(file: ReceivedFileEntity)
}

class LibraryRepository(
    private val context: Context,
    private val dao: ReceivedFileDao
) {
    private val appContext = context.applicationContext

    fun observeFiles(
        searchQuery: String = "",
        category: String? = null,
        sortOption: LibrarySortOption = LibrarySortOption.Newest
    ): Flow<List<ReceivedFileEntity>> = dao.observeFiltered(
        searchQuery = searchQuery.trim(),
        category = category,
        sortKey = sortOption.key
    )

    fun observeAllFiles(): Flow<List<ReceivedFileEntity>> = dao.observeAllByNewest()

    fun observeTotalSizeBytes(): Flow<Long> = dao.observeTotalSizeBytes()

    fun observeFileCount(): Flow<Int> = dao.observeFileCount()

    suspend fun isDuplicateHash(sha256Hash: String): Boolean =
        dao.existsBySha256(sha256Hash.trim())

    suspend fun findBySha256(sha256Hash: String): ReceivedFileEntity? =
        dao.findBySha256(sha256Hash.trim())

    suspend fun verifyIntegrity(file: ReceivedFileEntity): FileIntegrityResult {
        val computed = FileHashUtil.computeSha256FromPathOrUri(appContext, file.filePath)
            ?: return FileIntegrityResult.FileUnavailable
        return if (FileHashUtil.verifyIntegrity(computed, file.sha256Hash)) {
            FileIntegrityResult.Verified(computed)
        } else {
            FileIntegrityResult.Mismatch(expectedHash = file.sha256Hash, actualHash = computed)
        }
    }

    suspend fun indexReceivedFile(
        fileName: String,
        fileSizeBytes: Long,
        senderName: String,
        filePath: String,
        sha256Hash: String,
        receivedTimestamp: Long = System.currentTimeMillis(),
        isShareable: Boolean = true,
        skipIfDuplicate: Boolean = true
    ): Long? {
        if (skipIfDuplicate && sha256Hash.isNotBlank() && dao.existsBySha256(sha256Hash)) {
            return null
        }
        val entity = ReceivedFileEntity(
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            senderName = senderName.ifBlank { "Nearby Device" },
            filePath = filePath,
            category = StorageFormatter.classifyCategory(fileName),
            sha256Hash = sha256Hash,
            receivedTimestamp = receivedTimestamp,
            isShareable = isShareable
        )
        return dao.insert(entity)
    }

    suspend fun deleteFile(file: ReceivedFileEntity) = withContext(Dispatchers.IO) {
        dao.delete(file)
        runCatching {
            when {
                file.filePath.startsWith("content://") -> {
                    appContext.contentResolver.delete(Uri.parse(file.filePath), null, null)
                }

                file.filePath.startsWith("file://") -> {
                    Uri.parse(file.filePath).path?.let { File(it).delete() }
                }

                file.filePath.isNotBlank() -> {
                    File(file.filePath).delete()
                }
            }
        }
    }

    /**
     * Scans the local HOPPIE Downloads folder (MediaStore on API 29+ and app external Downloads
     * folder on older versions) and indexes any newly received files into Room with SHA-256 hashes.
     */
    suspend fun syncFromDeviceStorage() = withContext(Dispatchers.IO) {
        val senderLookup = loadKnownSenderByFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanMediaStoreDownloads(senderLookup)
        }
        scanAppLocalFolders(senderLookup)
    }

    private suspend fun scanMediaStoreDownloads(senderLookup: Map<String, String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_ADDED
        )
        val folderPaths = listOf(
            "${Environment.DIRECTORY_DOWNLOADS}/HOPPIE/",
            "${Environment.DIRECTORY_DOWNLOADS}/Hoppie/"
        )
        val selection = "(${MediaStore.Downloads.RELATIVE_PATH}=? OR ${MediaStore.Downloads.RELATIVE_PATH}=?) AND ${MediaStore.Downloads.IS_PENDING}=0"
        val args = folderPaths.toTypedArray()

        runCatching {
            appContext.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                    val uriString = uri.toString()
                    if (dao.findByFilePath(uriString) != null) continue
                    val name = cursor.getString(nameCol) ?: continue
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val dateMillis = cursor.getLong(dateCol).let { if (it > 0L) it * 1000L else System.currentTimeMillis() }
                    val hash = FileHashUtil.computeSha256FromPathOrUri(appContext, uriString) ?: continue
                    if (dao.existsBySha256(hash)) continue
                    dao.insert(
                        ReceivedFileEntity(
                            fileName = name,
                            fileSizeBytes = size,
                            senderName = senderLookup[name] ?: "Nearby Device",
                            filePath = uriString,
                            category = StorageFormatter.classifyCategory(name),
                            sha256Hash = hash,
                            receivedTimestamp = dateMillis,
                            isShareable = true
                        )
                    )
                }
            }
        }
    }

    private suspend fun scanAppLocalFolders(senderLookup: Map<String, String>) {
        val candidateDirs = listOfNotNull(
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "HOPPIE") },
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "Hoppie") },
            File(appContext.filesDir, "HOPPIE")
        ).distinctBy { it.absolutePath }

        for (dir in candidateDirs) {
            if (!dir.isDirectory) continue
            dir.listFiles().orEmpty().filter { it.isFile }.forEach { file ->
                val path = file.absolutePath
                if (dao.findByFilePath(path) != null) return@forEach
                val hash = runCatching { FileHashUtil.computeSha256(file) }.getOrNull() ?: return@forEach
                if (dao.existsBySha256(hash)) return@forEach
                dao.insert(
                    ReceivedFileEntity(
                        fileName = file.name,
                        fileSizeBytes = file.length(),
                        senderName = senderLookup[file.name] ?: "Nearby Device",
                        filePath = path,
                        category = StorageFormatter.classifyCategory(file.name),
                        sha256Hash = hash,
                        receivedTimestamp = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis(),
                        isShareable = true
                    )
                )
            }
        }
    }

    /**
     * Reads optional sender metadata recorded by completed transfers without coupling directly
     * to the Transfers feature package.
     */
    private fun loadKnownSenderByFileName(): Map<String, String> {
        val prefs = appContext.getSharedPreferences("completed_transfers", Context.MODE_PRIVATE)
        val result = mutableMapOf<String, String>()
        prefs.all.values.forEach { raw ->
            val json = raw as? String ?: return@forEach
            runCatching {
                val obj = JSONObject(json)
                if (!obj.optBoolean("received", false)) return@runCatching
                val peer = obj.optString("peerName").takeIf { it.isNotBlank() } ?: return@runCatching
                val files = obj.optJSONArray("files") ?: return@runCatching
                for (i in 0 until files.length()) {
                    val fileName = files.optJSONObject(i)?.optString("name").orEmpty()
                    if (fileName.isNotBlank()) result[fileName] = peer
                }
            }
        }
        return result
    }
}
