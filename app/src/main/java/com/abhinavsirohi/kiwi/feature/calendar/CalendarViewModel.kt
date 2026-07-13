package com.abhinavsirohi.kiwi.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhinavsirohi.kiwi.core.common.AppResult
import com.abhinavsirohi.kiwi.domain.model.NewTask
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.usecase.task.CreateTask
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveTasks
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val isCreatingTask: Boolean = false,
    val taskTitle: String = "",
    val isSaving: Boolean = false,
    val message: String? = null,
)

sealed interface CalendarTimelineItem {
    data class TaskEntry(val task: Task) : CalendarTimelineItem
    data class BreakEntry(val startMinutes: Int, val endMinutes: Int) : CalendarTimelineItem
}

fun buildTimelineItems(tasks: List<Task>): List<CalendarTimelineItem> {
    val timedTasks = tasks
        .filter { it.scheduledTimeMinutes != null }
        .sortedWith(compareBy<Task> { it.scheduledTimeMinutes }.thenBy { it.position }.thenBy { it.title })
    val timeline = mutableListOf<CalendarTimelineItem>()

    timedTasks.forEachIndexed { index, task ->
        timeline += CalendarTimelineItem.TaskEntry(task)
        val currentTime = task.scheduledTimeMinutes ?: return@forEachIndexed
        val nextTime = timedTasks.getOrNull(index + 1)?.scheduledTimeMinutes ?: return@forEachIndexed
        if (nextTime - currentTime >= MINIMUM_BREAK_MINUTES) {
            timeline += CalendarTimelineItem.BreakEntry(currentTime, nextTime)
        }
    }
    tasks
        .filter { it.scheduledTimeMinutes == null }
        .sortedWith(compareBy<Task> { it.position }.thenBy { it.title })
        .forEach { timeline += CalendarTimelineItem.TaskEntry(it) }
    return timeline
}

private const val MINIMUM_BREAK_MINUTES = 60

class CalendarViewModel(
    private val observeTasks: ObserveTasks,
    private val createTask: CreateTask,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    initialDate: LocalDate = LocalDate.now(),
) : ViewModel() {
    private var allTasks: List<Task> = emptyList()
    private val mutableState = MutableStateFlow(
        CalendarUiState(displayedMonth = YearMonth.from(initialDate), selectedDate = initialDate),
    )
    val state: StateFlow<CalendarUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            observeTasks().collectLatest { result ->
                when (result) {
                    is AppResult.Success -> {
                        allTasks = result.value
                        refreshTasks(message = null)
                    }
                    is AppResult.Failure -> mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        message = "Kiwi couldn’t load this day. Please try again.",
                    )
                }
            }
        }
    }

    fun showPreviousMonth() {
        mutableState.value = mutableState.value.copy(displayedMonth = mutableState.value.displayedMonth.minusMonths(1))
    }

    fun showNextMonth() {
        mutableState.value = mutableState.value.copy(displayedMonth = mutableState.value.displayedMonth.plusMonths(1))
    }

    fun selectDate(date: LocalDate) {
        val current = mutableState.value
        mutableState.value = current.copy(displayedMonth = YearMonth.from(date), selectedDate = date, message = null)
        refreshTasks(message = null)
    }

    fun startCreatingTask() {
        mutableState.value = mutableState.value.copy(isCreatingTask = true, taskTitle = "", message = null)
    }

    fun updateTaskTitle(title: String) {
        mutableState.value = mutableState.value.copy(taskTitle = title, message = null)
    }

    fun dismissTaskEditor() {
        if (!mutableState.value.isSaving) {
            mutableState.value = mutableState.value.copy(isCreatingTask = false, taskTitle = "", message = null)
        }
    }

    fun saveTask() {
        val current = mutableState.value
        if (current.isSaving) return
        if (current.taskTitle.isBlank()) {
            mutableState.value = current.copy(message = "Give this task a title.")
            return
        }
        mutableState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch(dispatcher) {
            when (createTask(NewTask(title = current.taskTitle, scheduledDate = current.selectedDate.toString()))) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(
                    isCreatingTask = false,
                    taskTitle = "",
                    isSaving = false,
                    message = null,
                )
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(
                    isSaving = false,
                    message = "Kiwi couldn’t save this task locally. Please try again.",
                )
            }
        }
    }

    private fun refreshTasks(message: String?) {
        val current = mutableState.value
        mutableState.value = current.copy(
            tasks = allTasks.filter { it.scheduledDate == current.selectedDate.toString() },
            isLoading = false,
            message = message,
        )
    }

    class Factory(
        private val observeTasks: ObserveTasks,
        private val createTask: CreateTask,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CalendarViewModel::class.java))
            return CalendarViewModel(observeTasks, createTask) as T
        }
    }
}
