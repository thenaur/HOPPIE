package com.example.hoppie.core.components

internal data class OfferedFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val sha256: String? = null
)

internal sealed interface FileOfferDecision {
    data class Accepted(val fileIds: Set<String>) : FileOfferDecision
    data object Declined : FileOfferDecision
    data object Ended : FileOfferDecision
}
