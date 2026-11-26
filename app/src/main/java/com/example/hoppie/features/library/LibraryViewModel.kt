package com.example.hoppie.features.library

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoppie.core.utils.FilePreviewHelper
import com.example.hoppie.di.LibraryContainer
import com.example.hoppie.data.local.entities.ReceivedFileEntity
import com.example.hoppie.data.repository.FileIntegrityResult
import com.example.hoppie.data.repository.LibrarySortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryContainer.libraryRepository(application)
    private val profilePrefs = application.getSharedPreferences("hoppie_profile_prefs", Context.MODE_PRIVATE)

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow(LibraryCategoryFilter.All)
    private val sortOption = MutableStateFlow(LibrarySortOption.Newest)
    private val viewMode = MutableStateFlow(LibraryViewMode.List)
    private val isSyncing = MutableStateFlow(false)
    private val previewState = MutableStateFlow<LibraryPreviewState?>(null)
    private val infoFile = MutableStateFlow<ReceivedFileEntity?>(null)
    private val isVerifyingIntegrity = MutableStateFlow(false)
    private val integrityResult = MutableStateFlow<FileIntegrityResult?>(null)
    private val deleteTarget = MutableStateFlow<ReceivedFileEntity?>(null)

    private val filteredFiles = combine(searchQuery, selectedCategory, sortOption) { query, category, sort ->
        Triple(query, category, sort)
    }.flatMapLatest { (query, category, sort) ->
        repository.observeFiles(
            searchQuery = query,
            category = category.categoryKey,
            sortOption = sort
        )
    }

    private data class ControlState(
        val query: String,
        val category: LibraryCategoryFilter,
        val sort: LibrarySortOption,
        val mode: LibraryViewMode,
        val syncing: Boolean
    )

    private data class DialogState(
        val preview: LibraryPreviewState?,
        val info: ReceivedFileEntity?,
        val verifying: Boolean,
        val integrity: FileIntegrityResult?,
        val delete: ReceivedFileEntity?
    )

    private val controlsFlow = combine(
        searchQuery,
        selectedCategory,
        sortOption,
        viewMode,
        isSyncing
    ) { query, category, sort, mode, syncing ->
        ControlState(query, category, sort, mode, syncing)
    }

    private val dialogsFlow = combine(
        previewState,
        infoFile,
        isVerifyingIntegrity,
        integrityResult,
        deleteTarget
    ) { preview, info, verifying, integrity, delete ->
        DialogState(preview, info, verifying, integrity, delete)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        filteredFiles,
        controlsFlow,
        dialogsFlow
    ) { files, controls, dialogs ->
        LibraryUiState(
            files = files,
            searchQuery = controls.query,
            selectedCategory = controls.category,
            sortOption = controls.sort,
            viewMode = controls.mode,
            isSyncing = controls.syncing,
            previewState = dialogs.preview,
            infoFile = dialogs.info,
            isVerifyingIntegrity = dialogs.verifying,
            integrityResult = dialogs.integrity,
            deleteTarget = dialogs.delete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LibraryUiState()
    )

    init {
        syncLocalStorage()
    }

    fun syncLocalStorage() {
        viewModelScope.launch {
            isSyncing.value = true
            try {
                repository.syncFromDeviceStorage()
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSelectCategory(category: LibraryCategoryFilter) {
        selectedCategory.value = category
    }

    fun onSelectSort(option: LibrarySortOption) {
        sortOption.value = option
    }

    fun onToggleViewMode() {
        viewMode.value = if (viewMode.value == LibraryViewMode.List) {
            LibraryViewMode.Grid
        } else {
            LibraryViewMode.List
        }
    }

    fun onOpenPreview(file: ReceivedFileEntity) {
        loadPreviewPage(file, pageIndex = 0)
    }

    fun onChangePdfPage(pageIndex: Int) {
        val current = previewState.value ?: return
        loadPreviewPage(current.file, pageIndex)
    }

    private fun loadPreviewPage(file: ReceivedFileEntity, pageIndex: Int) {
        previewState.value = LibraryPreviewState(
            file = file,
            isLoading = true,
            result = null,
            currentPage = pageIndex
        )
        viewModelScope.launch {
            val cachePreviews = profilePrefs.getBoolean("auto_cache_previews", true)
            val result = FilePreviewHelper.loadPreview(
                context = getApplication(),
                filePathOrUri = file.filePath,
                fileName = file.fileName,
                sha256Hash = file.sha256Hash,
                pageIndex = pageIndex,
                cachePreview = cachePreviews
            )
            if (previewState.value?.file?.id == file.id) {
                previewState.value = LibraryPreviewState(
                    file = file,
                    isLoading = false,
                    result = result,
                    currentPage = pageIndex
                )
            }
        }
    }

    fun onDismissPreview() {
        previewState.value = null
    }

    fun onOpenFileInfo(file: ReceivedFileEntity) {
        infoFile.value = file
        integrityResult.value = null
        isVerifyingIntegrity.value = false
    }

    fun onVerifyIntegrity(file: ReceivedFileEntity) {
        viewModelScope.launch {
            isVerifyingIntegrity.value = true
            try {
                integrityResult.value = repository.verifyIntegrity(file)
            } finally {
                isVerifyingIntegrity.value = false
            }
        }
    }

    fun onDismissFileInfo() {
        infoFile.value = null
        integrityResult.value = null
        isVerifyingIntegrity.value = false
    }

    fun onRequestDelete(file: ReceivedFileEntity) {
        deleteTarget.value = file
    }

    fun onDismissDelete() {
        deleteTarget.value = null
    }

    fun onConfirmDelete() {
        val target = deleteTarget.value ?: return
        deleteTarget.value = null
        viewModelScope.launch {
            repository.deleteFile(target)
        }
    }
}
