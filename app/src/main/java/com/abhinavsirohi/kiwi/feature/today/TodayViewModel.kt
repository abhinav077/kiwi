package com.abhinavsirohi.kiwi.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewTask
import com.abhinavsirohi.kiwi.domain.model.NewSubtask
import com.abhinavsirohi.kiwi.domain.model.PlannerSyncState
import com.abhinavsirohi.kiwi.domain.model.RecurrenceFrequency
import com.abhinavsirohi.kiwi.domain.model.RecurrenceRule
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import com.abhinavsirohi.kiwi.domain.model.TaskPriority
import com.abhinavsirohi.kiwi.domain.usecase.task.CreateTask
import com.abhinavsirohi.kiwi.domain.usecase.task.CreateSubtask
import com.abhinavsirohi.kiwi.domain.usecase.task.MoveSubtask
import com.abhinavsirohi.kiwi.domain.usecase.task.MoveSubtaskParameters
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveTasks
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveSubtasks
import com.abhinavsirohi.kiwi.domain.usecase.task.ObservePlannerSyncState
import com.abhinavsirohi.kiwi.domain.usecase.task.SaveTask
import com.abhinavsirohi.kiwi.domain.usecase.task.SaveSubtask
import com.abhinavsirohi.kiwi.domain.usecase.task.TombstoneTask
import com.abhinavsirohi.kiwi.domain.usecase.task.TombstoneSubtask
import com.abhinavsirohi.kiwi.domain.usecase.task.TombstoneTaskParameters
import com.abhinavsirohi.kiwi.domain.usecase.task.TombstoneSubtaskParameters
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class TaskDraft(
    val title: String = "",
    val description: String = "",
    val category: TaskCategory = TaskCategory.Personal,
    val priority: TaskPriority = TaskPriority.Normal,
    val notes: String = "",
    val scheduledDate: String = LocalDate.now().toString(),
    val timeText: String = "",
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.None,
    val recurrenceIntervalText: String = "1",
    val recurrenceEndDate: String = "",
)

data class SubtaskDraft(
    val taskLocalId: String,
    val editingSubtaskId: String? = null,
    val title: String = "",
)

data class TodayUiState(
    val tasks: List<Task> = emptyList(),
    val subtasks: Map<String, List<Subtask>> = emptyMap(),
    val isLoading: Boolean = true,
    val editor: TaskDraft? = null,
    val editingTaskId: String? = null,
    val pendingDelete: Task? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val subtaskEditor: SubtaskDraft? = null,
    val pendingDeleteSubtask: Subtask? = null,
    val syncState: PlannerSyncState = PlannerSyncState(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val observeTasks: ObserveTasks,
    private val observeSubtasks: ObserveSubtasks,
    private val observePlannerSyncState: ObservePlannerSyncState,
    private val createTask: CreateTask,
    private val createSubtask: CreateSubtask,
    private val saveTask: SaveTask,
    private val saveSubtask: SaveSubtask,
    private val tombstoneTask: TombstoneTask,
    private val tombstoneSubtask: TombstoneSubtask,
    private val moveSubtask: MoveSubtask,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            observeTasks()
                .flatMapLatest { result ->
                    val todayTasks = if (result is AppResult.Success) {
                        val today = LocalDate.now().toString()
                        result.value.filter { task -> task.scheduledDate == null || task.scheduledDate == today }
                    } else {
                        emptyList()
                    }
                    if (result is AppResult.Success && todayTasks.isNotEmpty()) {
                        combine(todayTasks.map { task -> observeSubtasks(task.localId) }) { results ->
                            todayTasks.associate { task ->
                                task.localId to (results[todayTasks.indexOf(task)] as? AppResult.Success<List<Subtask>>)?.value.orEmpty()
                            }
                        }.map { subtasks -> AppResult.Success(todayTasks) to subtasks }
                    } else {
                        flowOf(
                            (if (result is AppResult.Success) AppResult.Success(todayTasks) else result) to emptyMap(),
                        )
                    }
                }
                .collectLatest { (result, subtasks) ->
                    mutableState.value = when (result) {
                        is AppResult.Success -> mutableState.value.copy(
                            tasks = result.value,
                            subtasks = subtasks,
                            isLoading = false,
                            message = null,
                        )
                        is AppResult.Failure -> mutableState.value.copy(
                            isLoading = false,
                            message = "Kiwi couldn’t load your tasks. Please try again.",
                        )
                    }
                }
        }
        viewModelScope.launch(dispatcher) {
            observePlannerSyncState().collect { syncState ->
                mutableState.value = mutableState.value.copy(syncState = syncState)
            }
        }
    }

    fun startCreating() {
        mutableState.value = mutableState.value.copy(editor = TaskDraft(), editingTaskId = null, message = null)
    }

    fun startEditing(task: Task) {
        mutableState.value = mutableState.value.copy(
            editor = TaskDraft(
                title = task.title,
                description = task.description.orEmpty(),
                category = task.category,
                priority = task.priority,
                notes = task.notes.orEmpty(),
                scheduledDate = task.scheduledDate.orEmpty(),
                timeText = task.scheduledTimeMinutes?.toTimeText().orEmpty(),
                recurrenceFrequency = task.recurrenceRule.frequency,
                recurrenceIntervalText = task.recurrenceRule.interval.toString(),
                recurrenceEndDate = task.recurrenceRule.endDate.orEmpty(),
            ),
            editingTaskId = task.localId,
            message = null,
        )
    }

    fun updateDraft(transform: (TaskDraft) -> TaskDraft) {
        val draft = mutableState.value.editor ?: return
        mutableState.value = mutableState.value.copy(editor = transform(draft), message = null)
    }

    fun dismissEditor() {
        if (!mutableState.value.isSaving) {
            mutableState.value = mutableState.value.copy(editor = null, editingTaskId = null, message = null)
        }
    }

    fun saveEditor() {
        val current = mutableState.value
        val draft = current.editor ?: return
        if (current.isSaving) return
        val time = parseTime(draft.timeText)
        val recurrenceInterval = draft.recurrenceIntervalText.toIntOrNull()
        if (draft.title.isBlank()) {
            mutableState.value = current.copy(message = "Give this task a title.")
            return
        }
        if (draft.timeText.isNotBlank() && time == null) {
            mutableState.value = current.copy(message = "Use a time like 09:30, or leave it untimed.")
            return
        }
        if (draft.recurrenceFrequency != RecurrenceFrequency.None &&
            (recurrenceInterval == null || recurrenceInterval < 1)
        ) {
            mutableState.value = current.copy(message = "Repeat interval must be at least 1.")
            return
        }
        val recurrenceRule = RecurrenceRule(
            frequency = draft.recurrenceFrequency,
            interval = recurrenceInterval ?: 1,
            endDate = draft.recurrenceEndDate.trim().takeIf(String::isNotEmpty),
        )
        mutableState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch(dispatcher) {
            val existing = current.editingTaskId?.let { id -> current.tasks.firstOrNull { it.localId == id } }
            val result = if (existing == null) {
                createTask(
                    NewTask(
                        title = draft.title,
                        description = draft.description,
                        category = draft.category,
                        priority = draft.priority,
                        notes = draft.notes,
                        scheduledDate = draft.scheduledDate,
                        scheduledTimeMinutes = time,
                        recurrenceRule = recurrenceRule,
                    ),
                )
            } else {
                saveTask(
                    existing.copy(
                        title = draft.title,
                        description = draft.description,
                        category = draft.category,
                        priority = draft.priority,
                        notes = draft.notes,
                        scheduledDate = draft.scheduledDate,
                        scheduledTimeMinutes = time,
                        recurrenceRule = recurrenceRule,
                    ),
                )
            }
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(
                    editor = null,
                    editingTaskId = null,
                    isSaving = false,
                    message = null,
                )
                is AppResult.Failure -> mutableState.value.copy(
                    isSaving = false,
                    message = "Kiwi couldn’t save this task locally. Please try again.",
                )
            }
        }
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch(dispatcher) {
            when (saveTask(task.copy(isCompleted = !task.isCompleted))) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(
                    message = "Kiwi couldn’t update this task. Please try again.",
                )
            }
        }
    }

    fun requestDelete(task: Task) {
        mutableState.value = mutableState.value.copy(pendingDelete = task, message = null)
    }

    fun dismissDelete() {
        mutableState.value = mutableState.value.copy(pendingDelete = null)
    }

    fun confirmDelete() {
        val task = mutableState.value.pendingDelete ?: return
        viewModelScope.launch(dispatcher) {
            when (tombstoneTask(TombstoneTaskParameters(task.localId, currentTimeMillis()))) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(pendingDelete = null)
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(
                    pendingDelete = null,
                    message = "Kiwi couldn’t delete this task. Please try again.",
                )
            }
        }
    }

    fun startCreatingSubtask(task: Task) {
        mutableState.value = mutableState.value.copy(subtaskEditor = SubtaskDraft(task.localId), message = null)
    }

    fun startEditingSubtask(subtask: Subtask) {
        mutableState.value = mutableState.value.copy(
            subtaskEditor = SubtaskDraft(subtask.taskLocalId, subtask.localId, subtask.title),
            message = null,
        )
    }

    fun updateSubtaskDraft(title: String) {
        mutableState.value.subtaskEditor?.let { editor ->
            mutableState.value = mutableState.value.copy(subtaskEditor = editor.copy(title = title), message = null)
        }
    }

    fun dismissSubtaskEditor() {
        mutableState.value = mutableState.value.copy(subtaskEditor = null, message = null)
    }

    fun saveSubtaskEditor() {
        val editor = mutableState.value.subtaskEditor ?: return
        if (editor.title.isBlank()) {
            mutableState.value = mutableState.value.copy(message = "Give this subtask a title.")
            return
        }
        viewModelScope.launch(dispatcher) {
            val existing = editor.editingSubtaskId?.let { id -> mutableState.value.subtasks[editor.taskLocalId]?.firstOrNull { it.localId == id } }
            val result = if (existing == null) {
                createSubtask(NewSubtask(editor.taskLocalId, editor.title))
            } else {
                saveSubtask(existing.copy(title = editor.title))
            }
            mutableState.value = when (result) {
                is AppResult.Success -> mutableState.value.copy(subtaskEditor = null, message = null)
                is AppResult.Failure -> mutableState.value.copy(message = "Kiwi couldn’t save this subtask locally. Please try again.")
            }
        }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch(dispatcher) {
            if (saveSubtask(subtask.copy(isCompleted = !subtask.isCompleted)) is AppResult.Failure) {
                mutableState.value = mutableState.value.copy(message = "Kiwi couldn’t update this subtask. Please try again.")
            }
        }
    }

    fun moveSubtask(subtask: Subtask, direction: Int) {
        viewModelScope.launch(dispatcher) {
            if (moveSubtask(MoveSubtaskParameters(subtask.localId, direction)) is AppResult.Failure) {
                mutableState.value = mutableState.value.copy(message = "Kiwi couldn’t reorder this subtask. Please try again.")
            }
        }
    }

    fun requestDeleteSubtask(subtask: Subtask) {
        mutableState.value = mutableState.value.copy(pendingDeleteSubtask = subtask)
    }

    fun dismissDeleteSubtask() {
        mutableState.value = mutableState.value.copy(pendingDeleteSubtask = null)
    }

    fun confirmDeleteSubtask() {
        val subtask = mutableState.value.pendingDeleteSubtask ?: return
        viewModelScope.launch(dispatcher) {
            val result = tombstoneSubtask(TombstoneSubtaskParameters(subtask.localId, currentTimeMillis()))
            mutableState.value = mutableState.value.copy(
                pendingDeleteSubtask = null,
                message = if (result is AppResult.Failure) "Kiwi couldn’t delete this subtask. Please try again." else null,
            )
        }
    }

    class Factory(
        private val observeTasks: ObserveTasks,
        private val observeSubtasks: ObserveSubtasks,
        private val observePlannerSyncState: ObservePlannerSyncState,
        private val createTask: CreateTask,
        private val createSubtask: CreateSubtask,
        private val saveTask: SaveTask,
        private val saveSubtask: SaveSubtask,
        private val tombstoneTask: TombstoneTask,
        private val tombstoneSubtask: TombstoneSubtask,
        private val moveSubtask: MoveSubtask,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TodayViewModel::class.java))
            return TodayViewModel(
                observeTasks,
                observeSubtasks,
                observePlannerSyncState,
                createTask,
                createSubtask,
                saveTask,
                saveSubtask,
                tombstoneTask,
                tombstoneSubtask,
                moveSubtask,
            ) as T
        }
    }
}

private fun parseTime(value: String): Int? {
    if (value.isBlank()) return null
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}

private fun Int.toTimeText(): String = "%02d:%02d".format(this / 60, this % 60)
