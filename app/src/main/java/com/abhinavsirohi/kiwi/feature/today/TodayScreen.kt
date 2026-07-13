package com.abhinavsirohi.kiwi.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhinavsirohi.kiwi.KiwiApplication
import com.abhinavsirohi.kiwi.core.design.KiwiBackground
import com.abhinavsirohi.kiwi.core.design.KiwiButton
import com.abhinavsirohi.kiwi.core.design.KiwiCard
import com.abhinavsirohi.kiwi.core.design.KiwiChip
import com.abhinavsirohi.kiwi.data.repository.RoomTaskRepository
import com.abhinavsirohi.kiwi.domain.model.Task
import com.abhinavsirohi.kiwi.domain.model.TaskCategory
import com.abhinavsirohi.kiwi.domain.model.TaskPriority
import com.abhinavsirohi.kiwi.domain.model.Subtask
import com.abhinavsirohi.kiwi.domain.model.RecurrenceFrequency
import com.abhinavsirohi.kiwi.domain.model.PlannerSyncState
import com.abhinavsirohi.kiwi.domain.model.calculateSubtaskProgress
import com.abhinavsirohi.kiwi.domain.usecase.task.CreateSubtask
import com.abhinavsirohi.kiwi.domain.usecase.task.CreateTask
import com.abhinavsirohi.kiwi.domain.usecase.task.MoveSubtask
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveTasks
import com.abhinavsirohi.kiwi.domain.usecase.task.ObserveSubtasks
import com.abhinavsirohi.kiwi.domain.usecase.task.ObservePlannerSyncState
import com.abhinavsirohi.kiwi.domain.usecase.task.SaveTask
import com.abhinavsirohi.kiwi.domain.usecase.task.SaveSubtask
import com.abhinavsirohi.kiwi.domain.usecase.task.TombstoneTask
import com.abhinavsirohi.kiwi.domain.usecase.task.TombstoneSubtask
import com.abhinavsirohi.kiwi.ui.theme.KiwiButter
import com.abhinavsirohi.kiwi.ui.theme.KiwiCharcoal
import com.abhinavsirohi.kiwi.ui.theme.KiwiForest
import com.abhinavsirohi.kiwi.ui.theme.KiwiSpacing
import com.abhinavsirohi.kiwi.ui.theme.KiwiWarmGray

@Composable
fun TodayRoute(onAskKiwi: () -> Unit, modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as KiwiApplication
    val repository = RoomTaskRepository(
        application.database,
        application.supabaseClient,
        application.deviceId,
        application.taskReminderScheduler,
    )
    val todayViewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.Factory(
            observeTasks = ObserveTasks(repository),
            observeSubtasks = ObserveSubtasks(repository),
            observePlannerSyncState = ObservePlannerSyncState(repository),
            createTask = CreateTask(repository),
            createSubtask = CreateSubtask(repository),
            saveTask = SaveTask(repository),
            saveSubtask = SaveSubtask(repository),
            tombstoneTask = TombstoneTask(repository),
            tombstoneSubtask = TombstoneSubtask(repository),
            moveSubtask = MoveSubtask(repository),
        ),
    )
    val state by todayViewModel.state.collectAsState()
    TodayScreen(
        state = state,
        onAskKiwi = onAskKiwi,
        onAddTask = todayViewModel::startCreating,
        onEditTask = todayViewModel::startEditing,
        onToggleTask = todayViewModel::toggleCompleted,
        onDeleteTask = todayViewModel::requestDelete,
        onAddSubtask = todayViewModel::startCreatingSubtask,
        onEditSubtask = todayViewModel::startEditingSubtask,
        onToggleSubtask = todayViewModel::toggleSubtask,
        onMoveSubtask = todayViewModel::moveSubtask,
        onDeleteSubtask = todayViewModel::requestDeleteSubtask,
        onDraftChanged = todayViewModel::updateDraft,
        onSaveDraft = todayViewModel::saveEditor,
        onDismissEditor = todayViewModel::dismissEditor,
        onConfirmDelete = todayViewModel::confirmDelete,
        onDismissDelete = todayViewModel::dismissDelete,
        onSubtaskTitleChanged = todayViewModel::updateSubtaskDraft,
        onSaveSubtask = todayViewModel::saveSubtaskEditor,
        onDismissSubtaskEditor = todayViewModel::dismissSubtaskEditor,
        onConfirmDeleteSubtask = todayViewModel::confirmDeleteSubtask,
        onDismissDeleteSubtask = todayViewModel::dismissDeleteSubtask,
        modifier = modifier,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onAskKiwi: () -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddSubtask: (Task) -> Unit,
    onEditSubtask: (Subtask) -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
    onMoveSubtask: (Subtask, Int) -> Unit,
    onDeleteSubtask: (Subtask) -> Unit,
    onDraftChanged: ((TaskDraft) -> TaskDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onDismissEditor: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onSubtaskTitleChanged: (String) -> Unit,
    onSaveSubtask: () -> Unit,
    onDismissSubtaskEditor: () -> Unit,
    onConfirmDeleteSubtask: () -> Unit,
    onDismissDeleteSubtask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = state.tasks.filterNot(Task::isCompleted)
    val completed = state.tasks.count(Task::isCompleted)
    KiwiBackground(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = KiwiSpacing.lg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = KiwiSpacing.xl, bottom = KiwiSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(KiwiSpacing.md),
        ) {
            item { Greeting() }
            item { PlannerSyncStatus(state.syncState) }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item { NextTaskCard(active.minByOrNull { it.scheduledTimeMinutes ?: Int.MAX_VALUE }, onToggleTask) }
            item { ProgressCard(completed, state.tasks.size) }
            if (state.isLoading) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            } else if (state.tasks.isEmpty()) {
                item { EmptyTasks(onAddTask) }
            } else {
                val groups = state.tasks.groupBy(::timeGroup)
                listOf("Morning", "Afternoon", "Evening", "Untimed").forEach { group ->
                    groups[group]?.let { tasks ->
                        item { Text(group, style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal) }
                        items(tasks, key = Task::localId) { task ->
                            TaskCard(
                                task = task,
                                subtasks = state.subtasks[task.localId].orEmpty(),
                                onToggle = onToggleTask,
                                onEdit = onEditTask,
                                onDelete = onDeleteTask,
                                onAddSubtask = onAddSubtask,
                                onEditSubtask = onEditSubtask,
                                onToggleSubtask = onToggleSubtask,
                                onMoveSubtask = onMoveSubtask,
                                onDeleteSubtask = onDeleteSubtask,
                            )
                        }
                    }
                }
            }
            item { SelfCareMoment() }
            item { QuickActions(onAddTask, onAskKiwi) }
        }
    }
    state.editor?.let { draft ->
        TaskEditorDialog(draft, state.isSaving, state.message, onDraftChanged, onSaveDraft, onDismissEditor)
    }
    state.pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete this task?") },
            text = { Text("“${task.title}” will be removed from Today and queued for backup synchronization.") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("Keep task") } },
        )
    }
    state.subtaskEditor?.let { editor ->
        AlertDialog(
            onDismissRequest = onDismissSubtaskEditor,
            title = { Text(if (editor.editingSubtaskId == null) "Add subtask" else "Edit subtask") },
            text = {
                OutlinedTextField(
                    value = editor.title,
                    onValueChange = onSubtaskTitleChanged,
                    label = { Text("Subtask title") },
                    singleLine = true,
                )
            },
            confirmButton = { TextButton(onClick = onSaveSubtask) { Text("Save") } },
            dismissButton = { TextButton(onClick = onDismissSubtaskEditor) { Text("Cancel") } },
        )
    }
    state.pendingDeleteSubtask?.let { subtask ->
        AlertDialog(
            onDismissRequest = onDismissDeleteSubtask,
            title = { Text("Delete this subtask?") },
            text = { Text("“${subtask.title}” will be removed from this task.") },
            confirmButton = { TextButton(onClick = onConfirmDeleteSubtask) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismissDeleteSubtask) { Text("Keep") } },
        )
    }
}

@Composable
private fun Greeting() {
    Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.xxs)) {
        Text("Today", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
        Text("Good to see you", style = MaterialTheme.typography.displayLarge, color = KiwiCharcoal)
        Text("Let’s make space for what matters today.", style = MaterialTheme.typography.bodyLarge, color = KiwiWarmGray)
    }
}

@Composable
private fun PlannerSyncStatus(syncState: PlannerSyncState) {
    val message = when {
        syncState.failedCount > 0 -> "Saved on this device • sync will retry"
        syncState.processingCount > 0 -> "Saved on this device • syncing"
        syncState.pendingCount > 0 -> "Saved on this device • waiting to sync"
        else -> "Saved on this device"
    }
    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = if (syncState.failedCount > 0) MaterialTheme.colorScheme.error else KiwiForest,
    )
}

@Composable
private fun NextTaskCard(task: Task?, onToggle: (Task) -> Unit) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Text("NEXT UP", style = MaterialTheme.typography.labelSmall, color = KiwiForest)
        Spacer(Modifier.height(KiwiSpacing.xs))
        Text(task?.title ?: "Your day is open", style = MaterialTheme.typography.headlineLarge, color = KiwiCharcoal)
        Text(task?.description ?: "Add a task whenever you’re ready.", style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
        if (task != null) {
            Spacer(Modifier.height(KiwiSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.scheduleLabel(), style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
                Spacer(Modifier.weight(1f))
                KiwiButton(onClick = { onToggle(task) }) { Text("Done") }
            }
        }
    }
}

@Composable
private fun ProgressCard(completed: Int, total: Int) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today’s rhythm", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
            Spacer(Modifier.weight(1f))
            Text("$completed of $total complete", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
        }
        Spacer(Modifier.height(KiwiSpacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = KiwiButter,
            trackColor = KiwiButter.copy(alpha = 0.24f),
        )
    }
}

@Composable
private fun TaskCard(
    task: Task,
    subtasks: List<Subtask>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onAddSubtask: (Task) -> Unit,
    onEditSubtask: (Subtask) -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
    onMoveSubtask: (Subtask, Int) -> Unit,
    onDeleteSubtask: (Subtask) -> Unit,
) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (task.isCompleted) "✓" else "○", style = MaterialTheme.typography.titleLarge, color = KiwiForest)
            Spacer(Modifier.width(KiwiSpacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KiwiCharcoal,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                )
                Text("${task.category.name} • ${task.priority.name} • ${task.scheduleLabel()}", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
                task.recurrenceLabel()?.let { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall, color = KiwiForest)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onToggle(task) }) { Text(if (task.isCompleted) "Undo" else "Done") }
            TextButton(onClick = { onEdit(task) }) { Text("Edit") }
            TextButton(onClick = { onDelete(task) }) { Text("Delete") }
        }
        if (subtasks.isNotEmpty()) {
            val progress = calculateSubtaskProgress(subtasks)
            Text("Subtasks  ${subtasks.count(Subtask::isCompleted)} of ${subtasks.size}", style = MaterialTheme.typography.labelSmall, color = KiwiWarmGray)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp))
            subtasks.sortedBy(Subtask::position).forEachIndexed { index, subtask ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = subtask.isCompleted, onCheckedChange = { onToggleSubtask(subtask) })
                    Text(subtask.title, Modifier.weight(1f), textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null)
                    TextButton(onClick = { onMoveSubtask(subtask, -1) }, enabled = index > 0) { Text("↑") }
                    TextButton(onClick = { onMoveSubtask(subtask, 1) }, enabled = index < subtasks.lastIndex) { Text("↓") }
                    TextButton(onClick = { onEditSubtask(subtask) }) { Text("Edit") }
                    TextButton(onClick = { onDeleteSubtask(subtask) }) { Text("Delete") }
                }
            }
        }
        TextButton(onClick = { onAddSubtask(task) }) { Text("+ Add subtask") }
    }
}

@Composable
private fun EmptyTasks(onAddTask: () -> Unit) {
    KiwiCard(Modifier.fillMaxWidth()) {
        Text("A fresh page", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        Text("There are no tasks here yet.", style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
        Spacer(Modifier.height(KiwiSpacing.sm))
        KiwiButton(onClick = onAddTask) { Text("Add a task") }
    }
}

@Composable
private fun SelfCareMoment() {
    KiwiCard(Modifier.fillMaxWidth()) {
        Text("A moment for you", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        Text("Take a mindful pause when you need it.", style = MaterialTheme.typography.bodyMedium, color = KiwiWarmGray)
    }
}

@Composable
private fun QuickActions(onAddTask: () -> Unit, onAskKiwi: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
        Text("Quick actions", style = MaterialTheme.typography.titleLarge, color = KiwiCharcoal)
        KiwiChip(label = "+ Task", onClick = onAddTask)
        KiwiButton(onClick = onAskKiwi, modifier = Modifier.fillMaxWidth()) { Text("✦  Ask Kiwi") }
    }
}

@Composable
private fun TaskEditorDialog(
    draft: TaskDraft,
    isSaving: Boolean,
    message: String?,
    onChanged: ((TaskDraft) -> TaskDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task details") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.sm)) {
                item { OutlinedTextField(draft.title, { value -> onChanged { it.copy(title = value) } }, label = { Text("Title") }, singleLine = true) }
                item { OutlinedTextField(draft.description, { value -> onChanged { it.copy(description = value) } }, label = { Text("Description") }) }
                item { ChoiceRow("Category", TaskCategory.entries, draft.category) { value -> onChanged { it.copy(category = value) } } }
                item { ChoiceRow("Priority", TaskPriority.entries, draft.priority) { value -> onChanged { it.copy(priority = value) } } }
                item { OutlinedTextField(draft.scheduledDate, { value -> onChanged { it.copy(scheduledDate = value) } }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true) }
                item { OutlinedTextField(draft.timeText, { value -> onChanged { it.copy(timeText = value) } }, label = { Text("Time (HH:MM, optional)") }, singleLine = true) }
                item {
                    ChoiceRow("Repeat", RecurrenceFrequency.entries, draft.recurrenceFrequency) { value ->
                        onChanged { it.copy(recurrenceFrequency = value) }
                    }
                }
                if (draft.recurrenceFrequency != RecurrenceFrequency.None) {
                    item {
                        OutlinedTextField(
                            draft.recurrenceIntervalText,
                            { value -> onChanged { it.copy(recurrenceIntervalText = value) } },
                            label = { Text("Repeat every") },
                            singleLine = true,
                        )
                    }
                    item {
                        OutlinedTextField(
                            draft.recurrenceEndDate,
                            { value -> onChanged { it.copy(recurrenceEndDate = value) } },
                            label = { Text("Repeat end date (optional)") },
                            singleLine = true,
                        )
                    }
                }
                item { OutlinedTextField(draft.notes, { value -> onChanged { it.copy(notes = value) } }, label = { Text("Notes") }) }
                message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = !isSaving) { Text(if (isSaving) "Saving…" else "Save") } },
        dismissButton = { OutlinedButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } },
    )
}

@Composable
private fun <T : Enum<T>> ChoiceRow(label: String, values: List<T>, selected: T, onSelected: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.xs)) {
            values.forEach { value ->
                FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(value.name) })
            }
        }
    }
}

private fun timeGroup(task: Task): String = when (task.scheduledTimeMinutes) {
    null -> "Untimed"
    in 0 until 12 * 60 -> "Morning"
    in 12 * 60 until 17 * 60 -> "Afternoon"
    else -> "Evening"
}

private fun Task.scheduleLabel(): String = scheduledTimeMinutes?.let { minutes ->
    "%02d:%02d".format(minutes / 60, minutes % 60)
} ?: "Untimed"

private fun Task.recurrenceLabel(): String? {
    val rule = recurrenceRule
    if (rule.frequency == RecurrenceFrequency.None) return null
    val unit = when (rule.frequency) {
        RecurrenceFrequency.None -> return null
        RecurrenceFrequency.Daily -> "day"
        RecurrenceFrequency.Weekly -> "week"
        RecurrenceFrequency.Monthly -> "month"
    }
    val interval = if (rule.interval == 1) unit else "${rule.interval} ${unit}s"
    return "Repeats every $interval" + (rule.endDate?.let { " until $it" } ?: "")
}
