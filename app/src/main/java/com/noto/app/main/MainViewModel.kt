package com.noto.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noto.app.UiState
import com.noto.app.domain.model.Folder
import com.noto.app.domain.model.FolderIdWithNotesCount
import com.noto.app.domain.model.FolderListSortingType
import com.noto.app.domain.model.SortingOrder
import com.noto.app.domain.repository.FolderRepository
import com.noto.app.domain.repository.NoteRepository
import com.noto.app.domain.repository.SettingsRepository
import com.noto.app.getOrDefault
import com.noto.app.util.sorted
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val folderRepository: FolderRepository,
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val sortingType = settingsRepository.sortingType
        .stateIn(viewModelScope, SharingStarted.Lazily, FolderListSortingType.CreationDate)

    val sortingOrder = settingsRepository.sortingOrder
        .stateIn(viewModelScope, SharingStarted.Lazily, SortingOrder.Descending)

    val folders = combine(
        folderRepository.getFolders(),
        noteRepository.getFolderNotesCount(),
        sortingType,
        sortingOrder,
    ) { folders, notesCount, sortingType, sortingOrder ->
        folders
            .filter { it.parentId == null }
            .mapRecursively(folders, notesCount, sortingType, sortingOrder)
            .sorted(sortingType, sortingOrder)
    }
        .map { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)

    val archivedFolders = combine(
        folderRepository.getArchivedFolders(),
        noteRepository.getFolderNotesCount(),
        sortingType,
        sortingOrder,
    ) { folders, notesCount, sortingType, sortingOrder ->
        folders
            .filter { it.parentId == null }
            .mapRecursively(folders, notesCount, sortingType, sortingOrder)
            .sorted(sortingType, sortingOrder)
    }
        .map { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)

    val vaultedFolders = combine(
        folderRepository.getVaultedFolders(),
        noteRepository.getFolderNotesCount(),
        sortingType,
        sortingOrder,
    ) { folders, notesCount, sortingType, sortingOrder ->
        folders
            .filter { it.parentId == null }
            .mapRecursively(folders, notesCount, sortingType, sortingOrder)
            .sorted(sortingType, sortingOrder)
    }
        .map { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)

    val isVaultOpen = settingsRepository.isVaultOpen
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val vaultPasscode = settingsRepository.vaultPasscode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Temporary fix due to Koin creating multiple instances of [MainViewModel].
     * */
    val settingsVaultPasscode = settingsRepository.vaultPasscode

    val isBioAuthEnabled = settingsRepository.isBioAuthEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isShowNotesCount = settingsRepository.isShowNotesCount
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val allNotes = noteRepository.getAllMainNotes()
        .combine(folderRepository.getFolders()) { notes, folders ->
            notes.filter { note -> folders.any { folder -> folder.id == note.folderId } }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSortingType(value: FolderListSortingType) = viewModelScope.launch {
        settingsRepository.updateSortingType(value)
        if (value == FolderListSortingType.Manual)
            updateSortingOrder(SortingOrder.Ascending)
    }

    fun updateSortingOrder(value: SortingOrder) = viewModelScope.launch {
        settingsRepository.updateSortingOrder(value)
    }

    fun updateFolderPosition(folder: Folder, position: Int) = viewModelScope.launch {
        folderRepository.updateFolder(folder.copy(position = position))
    }

    fun updateFolderParentId(folder: Folder, parentId: Long?) = viewModelScope.launch {
        folderRepository.updateFolder(folder.copy(parentId = parentId))
    }

    fun openVault() = viewModelScope.launch {
        settingsRepository.updateIsVaultOpen(true)
    }

    fun closeVault() = viewModelScope.launch {
        settingsRepository.updateIsVaultOpen(false)
    }

    private fun List<Folder>.mapRecursively(
        allFolders: List<Folder>,
        foldersNotesCount: List<FolderIdWithNotesCount>,
        sortingType: FolderListSortingType,
        sortingOrder: SortingOrder,
    ): List<Pair<Folder, Int>> {
        return map { folder ->
            val notesCount = foldersNotesCount.firstOrNull { it.folderId == folder.id }?.notesCount ?: 0
            val childLibraries = allFolders
                .filter { it.parentId == folder.id }
                .mapRecursively(allFolders, foldersNotesCount, sortingType, sortingOrder)
                .sorted(sortingType, sortingOrder)
                .sortedByDescending { it.first.isPinned }
            folder.copy(folders = childLibraries) to notesCount
        }
    }
}