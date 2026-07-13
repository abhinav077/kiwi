package com.abhinavsirohi.kiwi.feature.selfcare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.model.SelfCareCategory
import com.abhinavsirohi.kiwi.domain.model.SelfCareDay
import com.abhinavsirohi.kiwi.domain.model.SelfCareRoutine
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.CreateSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.ObserveSelfCareRoutines
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.SaveSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.TombstoneSelfCareRoutine
import com.abhinavsirohi.kiwi.domain.usecase.selfcare.TombstoneSelfCareRoutineParameters
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SelfCareDraft(
    val name: String = "",
    val description: String = "",
    val category: SelfCareCategory = SelfCareCategory.Mind,
    val timeText: String = "",
    val repeatDays: Set<SelfCareDay> = emptySet(),
    val checklistText: String = "",
)

data class SelfCareUiState(
    val routines: List<SelfCareRoutine> = emptyList(),
    val isLoading: Boolean = true,
    val editor: SelfCareDraft? = null,
    val editingRoutineId: String? = null,
    val pendingDelete: SelfCareRoutine? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
)

class SelfCareViewModel(
    private val observeRoutines: ObserveSelfCareRoutines,
    private val createRoutine: CreateSelfCareRoutine,
    private val saveRoutineUseCase: SaveSelfCareRoutine,
    private val tombstoneRoutine: TombstoneSelfCareRoutine,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SelfCareUiState())
    val state: StateFlow<SelfCareUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            observeRoutines().collectLatest { result ->
                mutableState.value = when (result) {
                    is AppResult.Success -> mutableState.value.copy(routines = result.value, isLoading = false, message = null)
                    is AppResult.Failure -> mutableState.value.copy(isLoading = false, message = "Kiwi couldn’t load self-care routines. Please try again.")
                }
            }
        }
    }

    fun startCreating() { mutableState.value = mutableState.value.copy(editor = SelfCareDraft(), editingRoutineId = null, message = null) }

    fun startEditing(routine: SelfCareRoutine) {
        mutableState.value = mutableState.value.copy(
            editor = SelfCareDraft(routine.name, routine.description.orEmpty(), routine.category, routine.scheduledTimeMinutes?.toTimeText().orEmpty(), routine.repeatDays, routine.checklist.joinToString("\n")),
            editingRoutineId = routine.localId,
            message = null,
        )
    }

    fun updateDraft(update: (SelfCareDraft) -> SelfCareDraft) { mutableState.value = mutableState.value.copy(editor = mutableState.value.editor?.let(update), message = null) }
    fun dismissEditor() { if (!mutableState.value.isSaving) mutableState.value = mutableState.value.copy(editor = null, editingRoutineId = null) }

    fun saveEditor() {
        val current = mutableState.value
        val draft = current.editor ?: return
        val time = parseTime(draft.timeText)
        if (draft.name.isBlank()) { mutableState.value = current.copy(message = "Give this routine a name."); return }
        if (draft.timeText.isNotBlank() && time == null) { mutableState.value = current.copy(message = "Use a time like 09:30, or leave it untimed."); return }
        val checklist = draft.checklistText.lines().map(String::trim).filter(String::isNotEmpty)
        mutableState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch(dispatcher) {
            val existing = current.editingRoutineId?.let { id -> current.routines.firstOrNull { it.localId == id } }
            val result = existing?.let { saveRoutineUseCase(it.copy(name = draft.name, description = draft.description, category = draft.category, scheduledTimeMinutes = time, repeatDays = draft.repeatDays, checklist = checklist)) }
                ?: createRoutine(NewSelfCareRoutine(draft.name, draft.description, draft.category, time, draft.repeatDays, checklist))
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(editor = null, editingRoutineId = null, isSaving = false, message = null)
                is AppResult.Failure -> mutableState.value.copy(isSaving = false, message = "Kiwi couldn’t save this routine locally. Please try again.")
            }
        }
    }

    fun toggleActive(routine: SelfCareRoutine) { saveRoutine(routine.copy(isActive = !routine.isActive)) }

    fun toggleCompleted(routine: SelfCareRoutine) {
        val today = LocalDate.now().toString()
        val dates = routine.completionDates.toMutableSet().apply { if (!add(today)) remove(today) }
        saveRoutine(routine.copy(completionDates = dates))
    }

    private fun saveRoutine(routine: SelfCareRoutine) {
        viewModelScope.launch(dispatcher) {
            if (saveRoutineUseCase(routine) is AppResult.Failure) mutableState.value = mutableState.value.copy(message = "Kiwi couldn’t update this routine. Please try again.")
        }
    }

    fun requestDelete(routine: SelfCareRoutine) { mutableState.value = mutableState.value.copy(pendingDelete = routine) }
    fun dismissDelete() { mutableState.value = mutableState.value.copy(pendingDelete = null) }
    fun confirmDelete() {
        val routine = mutableState.value.pendingDelete ?: return
        viewModelScope.launch(dispatcher) {
            val result = tombstoneRoutine(TombstoneSelfCareRoutineParameters(routine.localId, now()))
            mutableState.value = mutableState.value.copy(pendingDelete = null, message = if (result is AppResult.Failure) "Kiwi couldn’t delete this routine. Please try again." else null)
        }
    }

    class Factory(private val observe: ObserveSelfCareRoutines, private val create: CreateSelfCareRoutine, private val save: SaveSelfCareRoutine, private val delete: TombstoneSelfCareRoutine) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SelfCareViewModel(observe, create, save, delete) as T
    }
}

private fun parseTime(value: String): Int? {
    if (value.isBlank()) return null
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    return (hours * 60 + minutes).takeIf { hours in 0..23 && minutes in 0..59 }
}

private fun Int.toTimeText() = "%02d:%02d".format(this / 60, this % 60)
