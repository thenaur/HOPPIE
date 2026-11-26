package com.example.hoppie.features.library

import com.example.hoppie.core.utils.FilePreviewResult
import com.example.hoppie.core.utils.StorageFormatter
import com.example.hoppie.data.local.entities.ReceivedFileEntity
import com.example.hoppie.data.repository.FileIntegrityResult
import com.example.hoppie.data.repository.LibrarySortOption

enum class LibraryCategoryFilter(val label: String, val categoryKey: String?) {
    All("All", null),
    Documents("Documents", StorageFormatter.CATEGORY_DOCUMENTS),
    Images("Images", StorageFormatter.CATEGORY_IMAGES),
    Code("Code", StorageFormatter.CATEGORY_CODE)
}

enum class LibraryViewMode {
    List,
    Grid
}

data class LibraryPreviewState(
    val file: ReceivedFileEntity,
    val isLoading: Boolean = true,
    val result: FilePreviewResult? = null,
    val currentPage: Int = 0
)

data class LibraryUiState(
    val files: List<ReceivedFileEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: LibraryCategoryFilter = LibraryCategoryFilter.All,
    val sortOption: LibrarySortOption = LibrarySortOption.Newest,
    val viewMode: LibraryViewMode = LibraryViewMode.List,
    val isSyncing: Boolean = false,
    val previewState: LibraryPreviewState? = null,
    val infoFile: ReceivedFileEntity? = null,
    val isVerifyingIntegrity: Boolean = false,
    val integrityResult: FileIntegrityResult? = null,
    val deleteTarget: ReceivedFileEntity? = null
)

/**
 * Sample items for Compose @Preview rendering only (not inserted into production repositories).
 */
internal val sampleLibraryPreviewFiles = listOf(
    ReceivedFileEntity(
        id = 1L,
        fileName = "Lecture_Week9.pdf",
        fileSizeBytes = 15L * 1024L * 1024L,
        senderName = "Athena",
        filePath = "Downloads/Hoppie/Lecture_Week9.pdf",
        category = StorageFormatter.CATEGORY_DOCUMENTS,
        sha256Hash = "a8f5f167f44f4964e6c998dee827110c4b1e3a81e2c52b97e29a8f10d4b7c219",
        receivedTimestamp = 1_764_120_000_000L,
        isShareable = true
    ),
    ReceivedFileEntity(
        id = 2L,
        fileName = "Activity2.pptx",
        fileSizeBytes = 10L * 1024L * 1024L,
        senderName = "Haidie",
        filePath = "Downloads/Hoppie/Activity2.pptx",
        category = StorageFormatter.CATEGORY_DOCUMENTS,
        sha256Hash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
        receivedTimestamp = 1_764_110_000_000L,
        isShareable = true
    ),
    ReceivedFileEntity(
        id = 3L,
        fileName = "Wireframe.png",
        fileSizeBytes = 2L * 1024L * 1024L,
        senderName = "Athena",
        filePath = "Downloads/Hoppie/Wireframe.png",
        category = StorageFormatter.CATEGORY_IMAGES,
        sha256Hash = "4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce",
        receivedTimestamp = 1_764_100_000_000L,
        isShareable = true
    ),
    ReceivedFileEntity(
        id = 4L,
        fileName = "Database.sql",
        fileSizeBytes = 1L * 1024L * 1024L,
        senderName = "Haidie",
        filePath = "Downloads/Hoppie/Database.sql",
        category = StorageFormatter.CATEGORY_CODE,
        sha256Hash = "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b",
        receivedTimestamp = 1_764_090_000_000L,
        isShareable = true
    )
)
