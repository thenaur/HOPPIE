package com.example.hoppie.core.components

import androidx.annotation.DrawableRes
import com.example.hoppie.R

internal enum class FileCategory(@field:DrawableRes val icon: Int) {
    Pdf(R.drawable.pdf_icon),
    Document(R.drawable.doc_icon),
    Image(R.drawable.images_icon),
    Presentation(R.drawable.ppt_icon),
    Code(R.drawable.programming_icon);

    companion object {
        fun from(name: String, mimeType: String? = null): FileCategory {
            return when (name.substringAfterLast('.', "").lowercase()) {
                "pdf" -> Pdf
                "ppt", "pptx", "odp" -> Presentation
                "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "heic", "tif", "tiff" -> Image
                "java", "py", "sql", "kt", "js", "ts", "c", "cpp", "h", "json", "xml", "html", "css", "ipynb" -> Code
                "doc", "docx", "txt", "rtf", "odt", "md", "xls", "xlsx", "ods", "csv" -> Document
                else -> when {
                    mimeType == "application/pdf" -> Pdf
                    mimeType?.startsWith("image/") == true -> Image
                    mimeType == "application/vnd.ms-powerpoint" ||
                        mimeType?.contains("presentation") == true -> Presentation
                    else -> Document
                }
            }
        }
    }
}
