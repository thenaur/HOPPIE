package com.example.hoppie.core.utils

import java.text.DecimalFormat
import java.util.Locale

object StorageFormatter {
    const val CATEGORY_DOCUMENTS = "Documents"
    const val CATEGORY_IMAGES = "Images"
    const val CATEGORY_CODE = "Code"

    private val documentExtensions = setOf(
        "pdf", "docx", "doc", "txt", "ppt", "pptx", "odp", "rtf", "odt", "md", "xls", "xlsx", "ods", "csv"
    )
    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg", "tif", "tiff"
    )
    private val codeExtensions = setOf(
        "java", "py", "sql", "kt", "kts", "js", "ts", "tsx", "jsx", "c", "cpp", "h", "hpp",
        "cs", "go", "rs", "rb", "php", "swift", "json", "xml", "html", "css", "sh", "yaml", "yml", "ipynb"
    )

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

    private fun formatNumber(value: Double): String {
        val rounded = DecimalFormat("#.#").format(value)
        return rounded.removeSuffix(".0")
    }

    fun classifyCategory(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            in documentExtensions -> CATEGORY_DOCUMENTS
            in imageExtensions -> CATEGORY_IMAGES
            in codeExtensions -> CATEGORY_CODE
            else -> CATEGORY_DOCUMENTS
        }
    }

    fun isPdf(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").equals("pdf", ignoreCase = true)

    fun isImage(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) in imageExtensions

    fun mimeTypeForFileName(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt", "md", "sql", "java", "py", "kt", "json", "xml", "html", "css", "csv" -> "text/plain"
            else -> "*/*"
        }
    }
}
