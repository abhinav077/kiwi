package com.abhinavsirohi.kiwi.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.DiaryEntry
import com.abhinavsirohi.kiwi.domain.model.NewDiaryEntry
import com.abhinavsirohi.kiwi.domain.model.DiaryPhoto
import com.abhinavsirohi.kiwi.domain.usecase.diary.CreateDiaryEntry
import com.abhinavsirohi.kiwi.domain.usecase.diary.ObserveDiaryEntries
import com.abhinavsirohi.kiwi.domain.usecase.diary.SaveDiaryEntry
import com.abhinavsirohi.kiwi.domain.usecase.diary.TombstoneDiaryEntry
import com.abhinavsirohi.kiwi.domain.usecase.diary.TombstoneDiaryEntryParameters
import com.abhinavsirohi.kiwi.domain.usecase.diary.AddDiaryPhoto
import com.abhinavsirohi.kiwi.domain.usecase.diary.AddDiaryPhotoParameters
import com.abhinavsirohi.kiwi.domain.usecase.diary.DeleteDiaryPhoto
import com.abhinavsirohi.kiwi.domain.usecase.diary.DeleteDiaryPhotoParameters
import com.abhinavsirohi.kiwi.domain.usecase.diary.ObserveDiaryPhotos
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DiaryDraft(
    val title: String = "",
    val content: String = "",
    val entryDate: String = LocalDate.now().toString(),
    val bestThing: String = "",
    val mood: String = "",
    val isFavourite: Boolean = false,
)

enum class DiaryFilter {
    ALL,
    FAVOURITES,
}

data class DiaryUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val query: String = "",
    val filter: DiaryFilter = DiaryFilter.ALL,
    val selectedDate: String? = null,
    val showCalendar: Boolean = false,
    val showPrivacy: Boolean = false,
    val isLoading: Boolean = true,
    val editor: DiaryDraft? = null,
    val editingEntryId: String? = null,
    val pendingDelete: DiaryEntry? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val photos: Map<String, List<DiaryPhoto>> = emptyMap(),
    val pendingDeletePhoto: DiaryPhoto? = null,
    val isAddingPhoto: Boolean = false,
)

class DiaryViewModel(
    private val observeEntries: ObserveDiaryEntries,
    private val createEntry: CreateDiaryEntry,
    private val saveEntry: SaveDiaryEntry,
    private val tombstoneEntry: TombstoneDiaryEntry,
    private val observePhotos: ObserveDiaryPhotos,
    private val addPhoto: AddDiaryPhoto,
    private val deletePhoto: DeleteDiaryPhoto,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DiaryUiState())
    val state: StateFlow<DiaryUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            observeEntries().collectLatest { result ->
                mutableState.value = when (result) {
                    is AppResult.Success -> mutableState.value.copy(entries = result.value, isLoading = false, message = null)
                    is AppResult.Failure -> mutableState.value.copy(isLoading = false, message = "Kiwi couldn’t load your diary. Please try again.")
                }
            }
        }
        viewModelScope.launch(dispatcher) {
            observePhotos().collectLatest { result ->
                mutableState.value = when (result) {
                    is AppResult.Success -> mutableState.value.copy(
                        photos = result.value.groupBy(DiaryPhoto::diaryEntryLocalId),
                    )
                    is AppResult.Failure -> mutableState.value.copy(
                        message = "Kiwi couldn’t load diary photos. Your writing is still available.",
                    )
                }
            }
        }
    }

    fun setQuery(query: String) { mutableState.value = mutableState.value.copy(query = query) }

    fun setFilter(filter: DiaryFilter) { mutableState.value = mutableState.value.copy(filter = filter) }

    fun toggleCalendar() {
        mutableState.value = mutableState.value.copy(showCalendar = !mutableState.value.showCalendar)
    }

    fun selectDate(date: LocalDate) {
        val value = date.toString()
        mutableState.value = mutableState.value.copy(
            selectedDate = if (mutableState.value.selectedDate == value) null else value,
            showCalendar = true,
        )
    }

    fun clearDate() { mutableState.value = mutableState.value.copy(selectedDate = null) }

    fun showPrivacy() { mutableState.value = mutableState.value.copy(showPrivacy = true) }

    fun dismissPrivacy() { mutableState.value = mutableState.value.copy(showPrivacy = false) }

    fun startCreating() { mutableState.value = mutableState.value.copy(editor = DiaryDraft(), editingEntryId = null, message = null) }

    fun startEditing(entry: DiaryEntry) {
        mutableState.value = mutableState.value.copy(
            editor = DiaryDraft(entry.title, entry.content, entry.entryDate, entry.bestThing.orEmpty(), entry.mood.orEmpty(), entry.isFavourite),
            editingEntryId = entry.localId,
            message = null,
        )
    }

    fun updateDraft(update: (DiaryDraft) -> DiaryDraft) {
        mutableState.value = mutableState.value.copy(editor = mutableState.value.editor?.let(update))
    }

    fun dismissEditor() { mutableState.value = mutableState.value.copy(editor = null, editingEntryId = null) }

    fun saveEditor() {
        val current = mutableState.value
        val draft = current.editor ?: return
        viewModelScope.launch(dispatcher) {
            mutableState.value = mutableState.value.copy(isSaving = true)
            val result = current.editingEntryId?.let { id ->
                current.entries.firstOrNull { it.localId == id }?.let { entry ->
                    saveEntry(entry.copy(title = draft.title, content = draft.content, entryDate = draft.entryDate, bestThing = draft.bestThing, mood = draft.mood, isFavourite = draft.isFavourite))
                } ?: AppResult.Failure(IllegalArgumentException("Diary entry was not found"))
            } ?: createEntry(NewDiaryEntry(draft.title, draft.content, draft.entryDate, draft.bestThing, draft.mood, draft.isFavourite))
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(editor = null, editingEntryId = null, isSaving = false, message = null)
                is AppResult.Failure -> mutableState.value.copy(isSaving = false, message = "Kiwi couldn’t save this diary entry. Add a title and some writing, then try again.")
            }
        }
    }

    fun requestDelete(entry: DiaryEntry) { mutableState.value = mutableState.value.copy(pendingDelete = entry) }
    fun dismissDelete() { mutableState.value = mutableState.value.copy(pendingDelete = null) }

    fun confirmDelete() {
        val entry = mutableState.value.pendingDelete ?: return
        viewModelScope.launch(dispatcher) {
            val result = tombstoneEntry(TombstoneDiaryEntryParameters(entry.localId, currentTimeMillis()))
            mutableState.value = mutableState.value.copy(
                pendingDelete = null,
                message = if (result is AppResult.Failure) "Kiwi couldn’t delete this entry. Please try again." else null,
            )
        }
    }

    fun addPhoto(diaryEntryLocalId: String, sourceUri: String) {
        viewModelScope.launch(dispatcher) {
            mutableState.value = mutableState.value.copy(isAddingPhoto = true, message = null)
            val result = addPhoto(AddDiaryPhotoParameters(diaryEntryLocalId, sourceUri))
            mutableState.value = mutableState.value.copy(
                isAddingPhoto = false,
                message = if (result is AppResult.Failure) {
                    "Kiwi couldn’t save this photo locally. Please choose another image and try again."
                } else {
                    "Photo saved on this device. Kiwi will back it up when online."
                },
            )
        }
    }

    fun requestDeletePhoto(photo: DiaryPhoto) {
        mutableState.value = mutableState.value.copy(pendingDeletePhoto = photo)
    }

    fun dismissDeletePhoto() {
        mutableState.value = mutableState.value.copy(pendingDeletePhoto = null)
    }

    fun confirmDeletePhoto() {
        val photo = mutableState.value.pendingDeletePhoto ?: return
        viewModelScope.launch(dispatcher) {
            val result = deletePhoto(DeleteDiaryPhotoParameters(photo.localId, currentTimeMillis()))
            mutableState.value = mutableState.value.copy(
                pendingDeletePhoto = null,
                message = if (result is AppResult.Failure) {
                    "Kiwi couldn’t remove this photo. Please try again."
                } else {
                    "Photo removed locally. Remote cleanup will finish when online."
                },
            )
        }
    }

    class Factory(
        private val observeEntries: ObserveDiaryEntries,
        private val createEntry: CreateDiaryEntry,
        private val saveEntry: SaveDiaryEntry,
        private val tombstoneEntry: TombstoneDiaryEntry,
        private val observePhotos: ObserveDiaryPhotos,
        private val addPhoto: AddDiaryPhoto,
        private val deletePhoto: DeleteDiaryPhoto,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DiaryViewModel(
            observeEntries, createEntry, saveEntry, tombstoneEntry, observePhotos, addPhoto, deletePhoto,
        ) as T
    }
}
