package com.example.hoppie.features.library.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppieLinkBlue
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.core.utils.FilePreviewHelper
import com.example.hoppie.core.utils.FilePreviewResult
import com.example.hoppie.core.utils.StorageFormatter
import com.example.hoppie.data.local.entities.ReceivedFileEntity
import com.example.hoppie.data.repository.FileIntegrityResult
import com.example.hoppie.features.library.LibraryPreviewState
import java.text.DateFormat
import java.util.Date

@Composable
internal fun FilePreviewDialog(
    state: LibraryPreviewState,
    onChangePdfPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val openExternally: () -> Unit = {
        val intent = FilePreviewHelper.buildExternalViewIntent(context, state.file.filePath, state.file.fileName)
        if (intent != null) {
            runCatching { context.startActivity(intent) }.onFailure {
                Toast.makeText(context, "No installed app can open this file type", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Local file is not available to open externally", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column {
                Text(
                    text = state.file.fileName,
                    color = HoppiePurple,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${StorageFormatter.formatBytes(state.file.fileSizeBytes)} · from ${state.file.senderName}",
                    color = HoppieFileSize,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 420.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = HoppiePurple)
                            Text("Loading preview...", color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    state.result is FilePreviewResult.PdfPage -> {
                        val pdf = state.result
                        val imageBitmap = remember(pdf.bitmap) { pdf.bitmap.asImageBitmap() }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "PDF page ${pdf.pageIndex + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 330.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF3F6FC)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onChangePdfPage(pdf.pageIndex - 1) },
                                    enabled = pdf.pageIndex > 0
                                ) {
                                    Text("Previous", style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    text = "Page ${pdf.pageIndex + 1} / ${pdf.pageCount}",
                                    color = HoppieFileSize,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(
                                    onClick = { onChangePdfPage(pdf.pageIndex + 1) },
                                    enabled = pdf.pageIndex < pdf.pageCount - 1
                                ) {
                                    Text("Next", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    state.result is FilePreviewResult.ImagePreview -> {
                        val imageBitmap = remember(state.result.bitmap) { state.result.bitmap.asImageBitmap() }
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = state.file.fileName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3F6FC)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    state.result is FilePreviewResult.Unsupported -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "In-app preview supports PDF and image files.",
                                color = HoppiePurple,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "You can open ${state.file.fileName} using a compatible app on your device.",
                                color = HoppieFileSize,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = openExternally,
                                colors = ButtonDefaults.buttonColors(containerColor = HoppiePurple)
                            ) {
                                Text("Open Externally", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    state.result is FilePreviewResult.MissingOrError -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Preview Unavailable",
                                color = HoppiePurple,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = state.result.message,
                                color = HoppieFileSize,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
internal fun FileInfoDialog(
    file: ReceivedFileEntity,
    isVerifying: Boolean,
    integrityResult: FileIntegrityResult?,
    onVerifyIntegrity: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember(file.receivedTimestamp) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(file.receivedTimestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text("File Info", color = HoppiePurple, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow("Name", file.fileName)
                InfoRow("Category", file.category)
                InfoRow("Size", StorageFormatter.formatBytes(file.fileSizeBytes))
                InfoRow("Sender", file.senderName)
                InfoRow("Received", formattedDate)
                InfoRow("Shareable", if (file.isShareable) "Yes (Ready for redistribution)" else "No")
                InfoRow("Local Path", file.filePath)

                HorizontalDivider(color = Color(0xFFECF1F8))

                Text("SHA-256 Hash", color = HoppiePurple, style = MaterialTheme.typography.labelLarge)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF3F6FC))
                        .padding(10.dp)
                ) {
                    Text(
                        text = file.sha256Hash.ifBlank { "Not computed" },
                        color = HoppiePurple,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                when (integrityResult) {
                    is FileIntegrityResult.Verified -> Text(
                        text = "Integrity verified: SHA-256 matches local file.",
                        color = Color(0xFF1E7E46),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    is FileIntegrityResult.Mismatch -> Text(
                        text = "Integrity mismatch: file hash differs from recorded SHA-256.",
                        color = Color(0xFFC93B3B),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    FileIntegrityResult.FileUnavailable -> Text(
                        text = "Local file could not be read for verification.",
                        color = HoppieFileSize,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    null -> Unit
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("SHA-256", file.sha256Hash))
                            Toast.makeText(context, "SHA-256 copied", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Copy Hash", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(
                        onClick = onVerifyIntegrity,
                        enabled = !isVerifying
                    ) {
                        Text(
                            text = if (isVerifying) "Verifying..." else "Verify Integrity",
                            color = HoppiePurple,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = HoppieLinkBlue, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(text = label, color = HoppieFileSize, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = HoppiePurple, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun DeleteFileConfirmDialog(
    file: ReceivedFileEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Delete File?", color = HoppiePurple, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(
                text = "Remove \"${file.fileName}\" from your HOPPIE Library and local storage?",
                color = HoppieFileSize,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color(0xFFC93B3B), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HoppieFileSize, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
