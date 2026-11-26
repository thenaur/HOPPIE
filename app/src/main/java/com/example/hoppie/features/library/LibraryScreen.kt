package com.example.hoppie.features.library

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoppie.R
import com.example.hoppie.core.theme.HOPPIETheme
import com.example.hoppie.core.theme.HoppieFileSize
import com.example.hoppie.core.theme.HoppiePurple
import com.example.hoppie.data.local.entities.ReceivedFileEntity
import com.example.hoppie.data.repository.FileRedistributionHook
import com.example.hoppie.data.repository.LibrarySortOption
import com.example.hoppie.features.library.components.DeleteFileConfirmDialog
import com.example.hoppie.features.library.components.FileInfoDialog
import com.example.hoppie.features.library.components.FilePreviewDialog
import com.example.hoppie.features.library.components.LibraryBrandHeader
import com.example.hoppie.features.library.components.LibraryCategoryChips
import com.example.hoppie.features.library.components.LibraryFileGridCard
import com.example.hoppie.features.library.components.LibraryFileListCard
import com.example.hoppie.features.library.components.LibrarySearchBar
import com.example.hoppie.features.library.components.LibrarySortAndLayoutRow
import com.example.hoppie.features.library.components.LibraryTitleSection

private val LibraryScreenBackground = Color(0xFFF6F8FC)

@Composable
internal fun LibraryScreen(
    onRedistributeFile: FileRedistributionHook? = null,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.syncLocalStorage()
    }

    LibraryScreenContent(
        state = state,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSelectCategory = viewModel::onSelectCategory,
        onSelectSort = viewModel::onSelectSort,
        onToggleViewMode = viewModel::onToggleViewMode,
        onPreviewFile = viewModel::onOpenPreview,
        onRedistributeFile = { file ->
            if (!file.isShareable) {
                Toast.makeText(context, "${file.fileName} is marked non-shareable", Toast.LENGTH_SHORT).show()
            } else if (onRedistributeFile != null) {
                onRedistributeFile.onRedistribute(file)
            } else {
                Toast.makeText(
                    context,
                    "Ready to redistribute ${file.fileName} via Nearby Send",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onShowFileInfo = viewModel::onOpenFileInfo,
        onRequestDeleteFile = viewModel::onRequestDelete
    )

    state.previewState?.let { previewState ->
        FilePreviewDialog(
            state = previewState,
            onChangePdfPage = viewModel::onChangePdfPage,
            onDismiss = viewModel::onDismissPreview
        )
    }

    state.infoFile?.let { infoFile ->
        FileInfoDialog(
            file = infoFile,
            isVerifying = state.isVerifyingIntegrity,
            integrityResult = state.integrityResult,
            onVerifyIntegrity = { viewModel.onVerifyIntegrity(infoFile) },
            onDismiss = viewModel::onDismissFileInfo
        )
    }

    state.deleteTarget?.let { target ->
        DeleteFileConfirmDialog(
            file = target,
            onDismiss = viewModel::onDismissDelete,
            onConfirm = viewModel::onConfirmDelete
        )
    }
}

@Composable
internal fun LibraryScreenContent(
    state: LibraryUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (LibraryCategoryFilter) -> Unit,
    onSelectSort: (LibrarySortOption) -> Unit,
    onToggleViewMode: () -> Unit,
    onPreviewFile: (ReceivedFileEntity) -> Unit,
    onRedistributeFile: (ReceivedFileEntity) -> Unit,
    onShowFileInfo: (ReceivedFileEntity) -> Unit,
    onRequestDeleteFile: (ReceivedFileEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LibraryScreenBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            LibraryBrandHeader()
            Spacer(Modifier.height(10.dp))
            LibraryTitleSection()
            Spacer(Modifier.height(14.dp))
            LibrarySearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange
            )
            Spacer(Modifier.height(14.dp))
            LibraryCategoryChips(
                selected = state.selectedCategory,
                onSelect = onSelectCategory
            )
            Spacer(Modifier.height(12.dp))
            LibrarySortAndLayoutRow(
                sortOption = state.sortOption,
                viewMode = state.viewMode,
                onSelectSort = onSelectSort,
                onToggleViewMode = onToggleViewMode
            )
            Spacer(Modifier.height(10.dp))

            if (state.files.isEmpty()) {
                LibraryEmptyState(
                    hasActiveFilter = state.searchQuery.isNotBlank() ||
                        state.selectedCategory != LibraryCategoryFilter.All
                )
            } else if (state.viewMode == LibraryViewMode.List) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.files.forEach { file ->
                        LibraryFileListCard(
                            file = file,
                            onPreview = { onPreviewFile(file) },
                            onRedistribute = { onRedistributeFile(file) },
                            onShowInfo = { onShowFileInfo(file) },
                            onDelete = { onRequestDeleteFile(file) }
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.files.chunked(2).forEach { rowFiles ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowFiles.forEach { file ->
                                LibraryFileGridCard(
                                    file = file,
                                    onPreview = { onPreviewFile(file) },
                                    onRedistribute = { onRedistributeFile(file) },
                                    onShowInfo = { onShowFileInfo(file) },
                                    onDelete = { onRequestDeleteFile(file) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowFiles.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LibraryEmptyState(hasActiveFilter: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE6ECF5), RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.hoppie_head),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = if (hasActiveFilter) "No matching files" else "No received files yet",
            color = HoppiePurple,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (hasActiveFilter) {
                "Try a different search keyword or category filter."
            } else {
                "Files you receive through HOPPIE will be indexed here automatically with SHA-256 verification."
            },
            color = HoppieFileSize,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    HOPPIETheme {
        LibraryScreenContent(
            state = LibraryUiState(files = sampleLibraryPreviewFiles),
            onSearchQueryChange = {},
            onSelectCategory = {},
            onSelectSort = {},
            onToggleViewMode = {},
            onPreviewFile = {},
            onRedistributeFile = {},
            onShowFileInfo = {},
            onRequestDeleteFile = {}
        )
    }
}
