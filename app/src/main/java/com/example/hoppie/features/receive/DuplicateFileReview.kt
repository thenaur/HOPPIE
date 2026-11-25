package com.example.hoppie.features.receive

import com.example.hoppie.core.components.OfferedFile
import java.util.Locale

/** Destination metadata supplied by the storage adapter. Digests describe actual stored content. */
internal data class StoredFile(val name: String, val sizeBytes: Long, val sha256: String?)

internal data class DuplicateFileReview(
    val remaining: List<OfferedFile>,
    private val existingNames: Set<String>,
    private val skippedIds: Set<String> = emptySet()
) {
    fun decide(skip: Boolean): DuplicateFileReview {
        val current = remaining.first()
        return copy(
            remaining = remaining.drop(1),
            skippedIds = if (skip) skippedIds + current.id else skippedIds
        )
    }

    fun destinations(files: List<OfferedFile>): Map<String, String> {
        check(remaining.isEmpty())
        val usedNames = existingNames.toMutableSet()
        return files.filterNot { it.id in skippedIds }.associate { file ->
            val name = file.name
            val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
            val stem = name.substring(0, dot)
            val extension = name.substring(dot)
            var candidate = name
            var copyNumber = 1
            while (!usedNames.add(candidate.lowercase(Locale.ROOT))) {
                candidate = "$stem (${copyNumber++})$extension"
            }
            file.id to candidate
        }
    }
}

internal fun reviewDuplicates(files: List<OfferedFile>, stored: List<StoredFile>): DuplicateFileReview {
    val digests = stored.mapNotNull { file ->
        file.sha256?.takeIf(Sha256Pattern::matches)?.let { file.sizeBytes to it.lowercase(Locale.ROOT) }
    }.toSet()
    val duplicates = files.filter { file ->
        val hash = file.sha256?.lowercase(Locale.ROOT)
        hash != null && (file.sizeBytes to hash) in digests
    }
    return DuplicateFileReview(duplicates, stored.mapTo(mutableSetOf()) { it.name.lowercase(Locale.ROOT) })
}

private val Sha256Pattern = Regex("[0-9a-fA-F]{64}")
