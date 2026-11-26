package com.example.hoppie.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface FilePreviewResult {
    data class PdfPage(
        val bitmap: Bitmap,
        val pageIndex: Int,
        val pageCount: Int
    ) : FilePreviewResult

    data class ImagePreview(
        val bitmap: Bitmap
    ) : FilePreviewResult

    data class Unsupported(
        val fileName: String,
        val mimeType: String,
        val canOpenExternally: Boolean
    ) : FilePreviewResult

    data class MissingOrError(
        val message: String
    ) : FilePreviewResult
}

object FilePreviewHelper {
    private const val PREVIEW_CACHE_DIR_NAME = "preview_cache"

    fun previewCacheDir(context: Context): File =
        File(context.cacheDir, PREVIEW_CACHE_DIR_NAME)

    suspend fun calculatePreviewCacheBytes(context: Context): Long = withContext(Dispatchers.IO) {
        val dir = previewCacheDir(context)
        if (!dir.exists()) return@withContext 0L
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun clearPreviewCache(context: Context): Long = withContext(Dispatchers.IO) {
        val dir = previewCacheDir(context)
        if (!dir.exists()) return@withContext 0L
        val cleared = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        dir.deleteRecursively()
        cleared
    }

    suspend fun loadPreview(
        context: Context,
        filePathOrUri: String,
        fileName: String,
        sha256Hash: String,
        pageIndex: Int = 0,
        cachePreview: Boolean = true,
        maxDimensionPx: Int = 1280
    ): FilePreviewResult = withContext(Dispatchers.IO) {
        when {
            StorageFormatter.isPdf(fileName) -> renderPdfPage(
                context = context,
                filePathOrUri = filePathOrUri,
                sha256Hash = sha256Hash,
                pageIndex = pageIndex,
                cachePreview = cachePreview,
                maxWidthPx = maxDimensionPx
            )

            StorageFormatter.isImage(fileName) -> decodeImagePreview(
                context = context,
                filePathOrUri = filePathOrUri,
                maxDimensionPx = maxDimensionPx
            )

            else -> FilePreviewResult.Unsupported(
                fileName = fileName,
                mimeType = StorageFormatter.mimeTypeForFileName(fileName),
                canOpenExternally = resolveShareableUri(context, filePathOrUri) != null
            )
        }
    }

    private fun renderPdfPage(
        context: Context,
        filePathOrUri: String,
        sha256Hash: String,
        pageIndex: Int,
        cachePreview: Boolean,
        maxWidthPx: Int
    ): FilePreviewResult {
        val pfd = openParcelFileDescriptor(context, filePathOrUri)
            ?: return FilePreviewResult.MissingOrError("Local PDF file could not be opened on this device.")

        return try {
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val count = renderer.pageCount
                    if (count <= 0) {
                        return FilePreviewResult.MissingOrError("This PDF document has no pages.")
                    }
                    val safeIndex = pageIndex.coerceIn(0, count - 1)
                    val cacheKey = "${sha256Hash.take(16)}_p$safeIndex.png"
                    val cachedFile = File(previewCacheDir(context), cacheKey)
                    if (cachePreview && cachedFile.isFile) {
                        BitmapFactory.decodeFile(cachedFile.absolutePath)?.let { cachedBitmap ->
                            return FilePreviewResult.PdfPage(cachedBitmap, safeIndex, count)
                        }
                    }

                    renderer.openPage(safeIndex).use { page ->
                        val scale = (maxWidthPx.toFloat() / page.width.coerceAtLeast(1)).coerceIn(0.5f, 2.5f)
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        if (cachePreview) {
                            runCatching {
                                previewCacheDir(context).mkdirs()
                                FileOutputStream(cachedFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                                }
                            }
                        }
                        FilePreviewResult.PdfPage(bitmap, safeIndex, count)
                    }
                }
            }
        } catch (t: Throwable) {
            FilePreviewResult.MissingOrError(t.message ?: "Unable to render PDF preview.")
        }
    }

    private fun decodeImagePreview(
        context: Context,
        filePathOrUri: String,
        maxDimensionPx: Int
    ): FilePreviewResult {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(context, filePathOrUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: return FilePreviewResult.MissingOrError("Local image file could not be opened on this device.")

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return FilePreviewResult.MissingOrError("Unable to decode image preview.")
            }

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDimensionPx || bounds.outHeight / sampleSize > maxDimensionPx) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = openStream(context, filePathOrUri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return FilePreviewResult.MissingOrError("Unable to decode image preview.")

            FilePreviewResult.ImagePreview(bitmap)
        } catch (t: Throwable) {
            FilePreviewResult.MissingOrError(t.message ?: "Unable to load image preview.")
        }
    }

    fun buildExternalViewIntent(context: Context, filePathOrUri: String, fileName: String): Intent? {
        val uri = resolveShareableUri(context, filePathOrUri) ?: return null
        val mime = StorageFormatter.mimeTypeForFileName(fileName)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun resolveShareableUri(context: Context, filePathOrUri: String): Uri? {
        if (filePathOrUri.isBlank()) return null
        return runCatching {
            when {
                filePathOrUri.startsWith("content://") -> Uri.parse(filePathOrUri)
                filePathOrUri.startsWith("file://") -> {
                    val file = File(Uri.parse(filePathOrUri).path ?: return null)
                    if (!file.isFile) return null
                    FileProvider.getUriForFile(context, "${context.packageName}.received", file)
                }

                else -> {
                    val file = File(filePathOrUri)
                    if (!file.isFile) return null
                    FileProvider.getUriForFile(context, "${context.packageName}.received", file)
                }
            }
        }.getOrNull()
    }

    private fun openParcelFileDescriptor(context: Context, filePathOrUri: String): ParcelFileDescriptor? {
        return runCatching {
            if (filePathOrUri.startsWith("content://") || filePathOrUri.startsWith("file://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(filePathOrUri), "r")
            } else {
                val file = File(filePathOrUri)
                if (!file.isFile) null else ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }.getOrNull()
    }

    private fun openStream(context: Context, filePathOrUri: String) = runCatching {
        if (filePathOrUri.startsWith("content://") || filePathOrUri.startsWith("file://")) {
            context.contentResolver.openInputStream(Uri.parse(filePathOrUri))
        } else {
            val file = File(filePathOrUri)
            if (!file.isFile) null else file.inputStream()
        }
    }.getOrNull()
}
